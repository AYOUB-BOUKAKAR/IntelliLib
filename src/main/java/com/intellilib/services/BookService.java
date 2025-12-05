package main.java.com.intellilib.services;

import com.intellilib.library.model.Book;
import com.intellilib.library.repository.BookRepository;

import java.util.List;

public class BookService {

    private final BookRepository repository = new BookRepository();

    public Book getBookById(Long id) {
        return repository.findById(id);
    }

    public List<Book> getAllBooks() {
        return repository.findAll();
    }

    public void addOrUpdateBook(Book book) {
        repository.save(book);
    }

    public void deleteBook(Long id) {
        repository.delete(id);
    }

    public List<Book> searchBooksByTitle(String title) {
        return repository.findByTitle(title);
    }
}
