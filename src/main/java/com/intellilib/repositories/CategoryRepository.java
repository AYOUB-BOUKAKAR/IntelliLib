package com.intellilib.repositories;

import com.intellilib.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // FREE: save(), findById(), findAll(), deleteById(), etc.
    
    // Find category by name
    Category findByName(String name);
    
    // Find categories containing name (case-insensitive)
    List<Category> findByNameContainingIgnoreCase(String name);
    
    // Check if category exists by name
    boolean existsByName(String name);
}