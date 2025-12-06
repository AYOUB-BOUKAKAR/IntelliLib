package com.intellilib.repositories;

import com.intellilib.models.Admin;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;

public class AdminRepository {

    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("intellilibPU");

    public Admin findById(Long id) {
        EntityManager em = emf.createEntityManager();
        Admin admin = em.find(Admin.class, id);
        em.close();
        return admin;
    }

    public List<Admin> findAll() {
        EntityManager em = emf.createEntityManager();
        List<Admin> list = em.createQuery("FROM Admin", Admin.class).getResultList();
        em.close();
        return list;
    }

    public void save(Admin admin) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        if (admin.getId() == null) em.persist(admin);
        else em.merge(admin);
        em.getTransaction().commit();
        em.close();
    }

    public void delete(Long id) {
        EntityManager em = emf.createEntityManager();
        Admin admin = em.find(Admin.class, id);
        if (admin != null) {
            em.getTransaction().begin();
            em.remove(admin);
            em.getTransaction().commit();
        }
        em.close();
    }

    public Admin findByUsername(String username) {
        EntityManager em = emf.createEntityManager();
        List<Admin> list = em.createQuery(
                "FROM Admin a WHERE a.username = :u", Admin.class)
                .setParameter("u", username)
                .getResultList();
        em.close();
        return list.isEmpty() ? null : list.get(0);
    }
}