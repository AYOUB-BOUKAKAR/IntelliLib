package com.intellilib.repositories;

import com.intellilib.models.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    List<Book> findByTitleContainingIgnoreCase(String title);
    
    List<Book> findByAuthorContainingIgnoreCase(String author);
    
    Optional<Book> findByIsbn(String isbn);
    
    List<Book> findByAvailableTrue();
    
    List<Book> findByAvailableFalse();
    
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Book> searchByTitleOrAuthor(String keyword);

    long countByAvailableTrue();

    long countByAvailableFalse();

    List<Book> findTop10ByOrderByAddedDateDesc();
    
    // Search by file type
    List<Book> findByFileType(String fileType);

    @Query("SELECT COUNT(b) FROM Book b WHERE b.addedDate < :date")
    long countByAddedDateBefore(@Param("date") LocalDate date);

    @Query("SELECT COUNT(b) FROM Book b WHERE b.addedDate >= :startDate AND b.addedDate < :endDate")
    long countByAddedDateBetween(@Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);
}