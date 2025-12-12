package com.intellilib.services;

import com.intellilib.models.Book;
import com.intellilib.repositories.BookRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookService {
    
    private final BookRepository bookRepository;
    
    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }
    
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }
    
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }
    
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
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

    // Optional: If you want to count only available books
    public long getAvailableBooksCount() {
        return bookRepository.countByAvailableTrue();
    }

    public long getUnavailableBooksCount() {
        return bookRepository.countByAvailableFalse();
    }

    public List<Book> findAvailableBooks(){
        return bookRepository.findByAvailableTrue();
    }
}