package com.bankx.service;

import com.bankx.entity.Customer;
import com.bankx.entity.User;
import com.bankx.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomerService customerService;

    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User createCustomerUser(String username, String email, String password, 
                                   String firstName, String lastName, String phoneNumber) {
        return createCustomerUser(username, email, password, firstName, lastName, phoneNumber, null);
    }

    public User createCustomerUser(String username, String email, String password, 
                                   String firstName, String lastName, String phoneNumber, String saIdNumber) {
        User user = new User(username, email, password, firstName, lastName, User.UserRole.CUSTOMER);
        user.setPhoneNumber(phoneNumber);
        user.setSaIdNumber(saIdNumber);
        user = createUser(user);

        Customer customer = new Customer(firstName, lastName, email, phoneNumber, saIdNumber);
        customer.setUser(user);
        customer = customerService.registerCustomer(customer);

        user.setCustomer(customer);
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
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
}