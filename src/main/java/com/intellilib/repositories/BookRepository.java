package com.intellilib.repositories;

import com.intellilib.models.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    // FREE METHODS from JpaRepository:
    // - save(Book book)
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)
    // - count()
    // - existsById(Long id)
    
    // Custom methods - Spring writes SQL for you!
    
    // Search by title (case-insensitive, partial match)
    List<Book> findByTitleContainingIgnoreCase(String title);
    
    // Search by author (case-insensitive)
    List<Book> findByAuthorContainingIgnoreCase(String author);
    
    // Find by ISBN
    Optional<Book> findByIsbn(String isbn);
    
    // Find available books
    List<Book> findByAvailableTrue();
    
    // Find unavailable books
    List<Book> findByAvailableFalse();
    
    // Search by title OR author
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Book> searchByTitleOrAuthor(String keyword);

    long countByAvailableTrue();
    long countByAvailableFalse();
}