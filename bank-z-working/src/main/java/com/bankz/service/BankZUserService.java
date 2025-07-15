package com.bankz.service;

import com.bankz.entity.BankZUser;
import com.bankz.repository.BankZUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BankZUserService {
    
    @Autowired
    private BankZUserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public BankZUser createUser(String username, String email, String password, String firstName, String lastName, BankZUser.UserRole role) {
        BankZUser user = new BankZUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setEnabled(true);
        
        return userRepository.save(user);
    }
    
    public BankZUser createUser(BankZUser user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
    
    public Optional<BankZUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public boolean existsBySaIdNumber(String saIdNumber) {
        return userRepository.existsBySaIdNumber(saIdNumber);
    }
    
    public List<BankZUser> getAllUsers() {
        return userRepository.findAll();
    }
}