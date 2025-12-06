package com.intellilib.services;

import com.intellilib.models.Borrow;
import com.intellilib.repositories.BorrowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BorrowService {

    @Autowired
    private BorrowRepository borrowRepository;

    public Borrow save(Borrow borrow) {
        return borrowRepository.save(borrow);
    }

    public Borrow update(Borrow borrow) {
        return borrowRepository.save(borrow);
    }

    public void delete(Long id) {
        borrowRepository.deleteById(id);
    }

    public Optional<Borrow> findById(Long id) {
        return borrowRepository.findById(id);
    }

    public List<Borrow> findAll() {
        return borrowRepository.findAll();
    }
}