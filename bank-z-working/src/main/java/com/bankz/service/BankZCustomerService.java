package com.bankz.service;

import com.bankz.entity.BankZCustomer;
import com.bankz.repository.BankZCustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BankZCustomerService {
    
    @Autowired
    private BankZCustomerRepository customerRepository;
    
    public BankZCustomer createCustomer(String firstName, String lastName, String email, String phoneNumber, String idNumber, String address) {
        BankZCustomer customer = new BankZCustomer(firstName, lastName, email, phoneNumber, idNumber, address);
        return customerRepository.save(customer);
    }
    
    public List<BankZCustomer> getAllCustomers() {
        return customerRepository.findAll();
    }
    
    public Optional<BankZCustomer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }
    
    public Optional<BankZCustomer> getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }
    
    public Optional<BankZCustomer> getCustomerByIdNumber(String idNumber) {
        return customerRepository.findByIdNumber(idNumber);
    }
    
    public boolean existsByEmail(String email) {
        return customerRepository.existsByEmail(email);
    }
    
    public boolean existsByIdNumber(String idNumber) {
        return customerRepository.existsByIdNumber(idNumber);
    }
}