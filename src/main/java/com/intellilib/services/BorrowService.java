package com.intellilib.services;

import com.intellilib.models.Borrow;
import com.intellilib.repositories.BorrowRepository;
import java.util.List;

public class BorrowService {

    private final BorrowRepository repository = new BorrowRepository();

    public Borrow getBorrowById(Long id) {
        return repository.findById(id);
    }

    public List<Borrow> getAllBorrows() {
        return repository.findAll();
    }

    public void addOrUpdateBorrow(Borrow borrow) {
        repository.save(borrow);
    }

    public void deleteBorrow(Long id) {
        repository.delete(id);
    }
}

