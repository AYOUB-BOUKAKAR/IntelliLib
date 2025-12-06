package com.intellilib.repositories;

import com.intellilib.models.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;

public class MemberRepository {

    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("intellilibPU");

    public Member findById(Long id) {
        EntityManager em = emf.createEntityManager();
        Member member = em.find(Member.class, id);
        em.close();
        return member;
    }

    public List<Member> findAll() {
        EntityManager em = emf.createEntityManager();
        List<Member> members = em.createQuery("SELECT m FROM Member m", Member.class).getResultList();
        em.close();
        return members;
    }

    public void save(Member member) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        if (member.getId() == null) {
            em.persist(member);
        } else {
            em.merge(member);
        }
        em.getTransaction().commit();
        em.close();
    }

    public void delete(Long id) {
        EntityManager em = emf.createEntityManager();
        Member member = em.find(Member.class, id);
        if (member != null) {
            em.getTransaction().begin();
            em.remove(member);
            em.getTransaction().commit();
        }
        em.close();
    }

    public Member findByEmail(String email) {
        EntityManager em = emf.createEntityManager();
        List<Member> result = em.createQuery(
                        "SELECT m FROM Member m WHERE LOWER(m.email) = LOWER(:email)", Member.class)
                .setParameter("email", email)
                .getResultList();
        em.close();
        return result.isEmpty() ? null : result.get(0);
    }
}
