package com.intellilib.repositories;

import com.intellilib.models.Book;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;

public class BookRepository {

    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("intellilibPU");

    public Book findById(Long id) {
        EntityManager em = emf.createEntityManager();
        Book book = em.find(Book.class, id);
        em.close();
        return book;
    }

    public List<Book> findAll() {
        EntityManager em = emf.createEntityManager();
        List<Book> books = em.createQuery("SELECT b FROM Book b", Book.class).getResultList();
        em.close();
        return books;
    }

    public void save(Book book) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        if (book.getId() == null) {
            em.persist(book);
        } else {
            em.merge(book);
        }
        em.getTransaction().commit();
        em.close();
    }

    public void delete(Long id) {
        EntityManager em = emf.createEntityManager();
        Book book = em.find(Book.class, id);
        if (book != null) {
            em.getTransaction().begin();
            em.remove(book);
            em.getTransaction().commit();
        }
        em.close();
    }

    public List<Book> findByTitle(String title) {
        EntityManager em = emf.createEntityManager();
        List<Book> books = em.createQuery(
                        "SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(:title)", Book.class)
                .setParameter("title", "%" + title + "%")
                .getResultList();
        em.close();
        return books;
    }
}