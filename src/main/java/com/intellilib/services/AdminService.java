package com.intellilib.services;

import com.intellilib.models.Admin;
import com.intellilib.repositories.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    public Admin save(Admin admin) {
        return adminRepository.save(admin);
    }

    public Admin update(Admin admin) {
        return adminRepository.save(admin);
    }

    public void delete(Long id) {
        adminRepository.deleteById(id);
    }

    public Optional<Admin> findById(Long id) {
        return adminRepository.findById(id);
    }

    public List<Admin> findAll() {
        return adminRepository.findAll();
    }
}