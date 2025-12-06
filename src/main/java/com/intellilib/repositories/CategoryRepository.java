package com.intellilib.repositories;

import com.intellilib.models.Category;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;

public class CategoryRepository {

    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("intellilibPU");

    public Category findById(Long id) {
        EntityManager em = emf.createEntityManager();
        Category category = em.find(Category.class, id);
        em.close();
        return category;
    }

    public List<Category> findAll() {
        EntityManager em = emf.createEntityManager();
        List<Category> categories = em.createQuery("SELECT c FROM Category c", Category.class).getResultList();
        em.close();
        return categories;
    }

    public void save(Category category) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        if (category.getId() == null) {
            em.persist(category);
        } else {
            em.merge(category);
        }
        em.getTransaction().commit();
        em.close();
    }

    public void delete(Long id) {
        EntityManager em = emf.createEntityManager();
        Category category = em.find(Category.class, id);
        if (category != null) {
            em.getTransaction().begin();
            em.remove(category);
            em.getTransaction().commit();
        }
        em.close();
    }
}
