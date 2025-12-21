package com.intellilib.services;

import com.intellilib.models.Book;
import com.intellilib.repositories.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookService {
    
    private final BookRepository bookRepository;
    private final FileStorageService fileStorageService;
    
    @Transactional
    public Book saveBook(Book book, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            // Store file and update book info
            String filePath = fileStorageService.storeFile(file, book.getIsbn());
            book.setFilePath(filePath);
            book.setOriginalFileName(file.getOriginalFilename());
            book.setFileSize(file.getSize());
            book.setFileType(fileStorageService.getMimeType(file.getOriginalFilename()));
        }
        
        return bookRepository.save(book);
    }
    
    @Transactional
    public Book updateBook(Long id, Book bookDetails, MultipartFile file) throws IOException {
        Optional<Book> optionalBook = bookRepository.findById(id);
        if (optionalBook.isEmpty()) {
            throw new RuntimeException("Book not found");
        }
        
        Book book = optionalBook.get();
        
        // Update fields
        book.setTitle(bookDetails.getTitle());
        book.setAuthor(bookDetails.getAuthor());
        book.setIsbn(bookDetails.getIsbn());
        book.setPublicationYear(bookDetails.getPublicationYear());
        book.setPublisher(bookDetails.getPublisher());
        book.setQuantity(bookDetails.getQuantity());
        book.setCategory(bookDetails.getCategory());
        book.setDescription(bookDetails.getDescription());
        book.setAvailable(bookDetails.isAvailable());
        
        // Handle file update
        if (file != null && !file.isEmpty()) {
            // Delete old file if exists
            if (book.getFilePath() != null) {
                fileStorageService.deleteFile(book.getFilePath());
            }
            
            // Store new file
            String filePath = fileStorageService.storeFile(file, book.getIsbn());
            book.setFilePath(filePath);
            book.setOriginalFileName(file.getOriginalFilename());
            book.setFileSize(file.getSize());
            book.setFileType(fileStorageService.getMimeType(file.getOriginalFilename()));
        }
        
        return bookRepository.save(book);
    }
    
    @Transactional
    public void deleteBook(Long id) throws IOException {
        Optional<Book> optionalBook = bookRepository.findById(id);
        if (optionalBook.isPresent()) {
            Book book = optionalBook.get();
            
            // Delete associated file
            if (book.getFilePath() != null) {
                fileStorageService.deleteFile(book.getFilePath());
            }
            
            bookRepository.deleteById(id);
        }
    }
    
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }
    
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }
    
    public List<Book> searchBooks(String keyword) {
        return bookRepository.searchByTitleOrAuthor(keyword);
    }
    
    @Transactional
    public boolean borrowBook(Long bookId) {
        Optional<Book> optionalBook = bookRepository.findById(bookId);
        if (optionalBook.isPresent() && optionalBook.get().isAvailable()) {
            Book book = optionalBook.get();
            book.setAvailable(false);
            bookRepository.save(book);
            return true;
        }
        return false;
    }
    
    @Transactional
    public boolean returnBook(Long bookId) {
        Optional<Book> optionalBook = bookRepository.findById(bookId);
        if (optionalBook.isPresent() && !optionalBook.get().isAvailable()) {
            Book book = optionalBook.get();
            book.setAvailable(true);
            bookRepository.save(book);
            return true;
        }
        return false;
    }

    public long getTotalBooksCount() {
        return bookRepository.count();
    }

    public long getAvailableBooksCount() {
        return bookRepository.countByAvailableTrue();
    }

    public long getUnavailableBooksCount() {
        return bookRepository.countByAvailableFalse();
    }

    public List<Book> findAvailableBooks(){
        return bookRepository.findByAvailableTrue();
    }

    public List<Book> getRecentBooks(int limit) {
        return bookRepository.findTop10ByOrderByAddedDateDesc();
    }

    public java.nio.file.Path getBookFilePath(Long bookId) {
        Optional<Book> book = bookRepository.findById(bookId);
        return book.map(b -> fileStorageService.loadFile(b.getFilePath()))
                  .orElse(null);
    }

    public double getTotalBooksChangeFromLastMonth() {
        LocalDate today = LocalDate.now();

        // Get current month start and end
        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate nextMonthStart = currentMonthStart.plusMonths(1);

        // Get previous month start and end
        LocalDate lastMonthStart = currentMonthStart.minusMonths(1);
        LocalDate lastMonthEnd = currentMonthStart.minusDays(1);

        // Count books added in current month (up to today)
        long currentMonthBooks = bookRepository.countByAddedDateBetween(
                currentMonthStart,
                today.plusDays(1) // Include today
        );

        // Count books added in previous month
        long lastMonthBooks = bookRepository.countByAddedDateBetween(
                lastMonthStart,
                lastMonthEnd.plusDays(1) // Include last day
        );

        // Calculate percentage change
        return calculatePercentageChange(currentMonthBooks, lastMonthBooks);
    }

    public long getBooksAddedThisMonth() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        return bookRepository.countByAddedDateBetween(monthStart, today.plusDays(1));
    }

    public long getBooksAddedLastMonth() {
        LocalDate today = LocalDate.now();
        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = currentMonthStart.minusMonths(1);
        LocalDate lastMonthEnd = currentMonthStart.minusDays(1);

        return bookRepository.countByAddedDateBetween(lastMonthStart, lastMonthEnd.plusDays(1));
    }

    private double calculatePercentageChange(double current, double previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((current - previous) / previous) * 100;
    }
}