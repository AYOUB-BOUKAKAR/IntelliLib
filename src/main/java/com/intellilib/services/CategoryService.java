package com.intellilib.services;

import com.intellilib.models.Category;
import com.intellilib.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor  // Better than @Autowired - creates constructor automatically
@Transactional  // All methods run in transaction
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Category createCategory(Category category) {
        // Check if category name already exists
        if (categoryRepository.existsByName(category.getName())) {
            throw new RuntimeException("Category with name '" + category.getName() + "' already exists");
        }
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, Category categoryDetails) {
        return categoryRepository.findById(id)
                .map(existingCategory -> {
                    // Update only if name is different
                    if (!existingCategory.getName().equals(categoryDetails.getName())) {
                        // Check if new name already exists
                        if (categoryRepository.existsByName(categoryDetails.getName())) {
                            throw new RuntimeException("Category name '" + categoryDetails.getName() + "' already exists");
                        }
                        existingCategory.setName(categoryDetails.getName());
                    }
                    
                    existingCategory.setDescription(categoryDetails.getDescription());
                    return categoryRepository.save(existingCategory);
                })
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category getCategoryByIdOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    public List<Category> searchCategories(String keyword) {
        return categoryRepository.findByNameContainingIgnoreCase(keyword);
    }

    public boolean categoryExists(String name) {
        return categoryRepository.existsByName(name);
    }
}