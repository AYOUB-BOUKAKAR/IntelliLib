package com.intellilib.repositories;

import com.intellilib.models.Borrow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;

public class BorrowRepository {

    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("intellilibPU");

    public Borrow findById(Long id) {
        EntityManager em = emf.createEntityManager();
        Borrow borrow = em.find(Borrow.class, id);
        em.close();
        return borrow;
    }

    public List<Borrow> findAll() {
        EntityManager em = emf.createEntityManager();
        List<Borrow> list = em.createQuery("FROM Borrow", Borrow.class).getResultList();
        em.close();
        return list;
    }

    public void save(Borrow borrow) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        if (borrow.getId() == null) em.persist(borrow);
        else em.merge(borrow);
        em.getTransaction().commit();
        em.close();
    }

    public void delete(Long id) {
        EntityManager em = emf.createEntityManager();
        Borrow borrow = em.find(Borrow.class, id);
        if (borrow != null) {
            em.getTransaction().begin();
            em.remove(borrow);
            em.getTransaction().commit();
        }
        em.close();
    }
}