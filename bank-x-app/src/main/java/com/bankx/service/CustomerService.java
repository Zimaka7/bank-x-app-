package com.bankx.service;

import com.bankx.dto.CustomerRegistrationRequest;
import com.bankx.dto.CustomerResponse;
import com.bankx.entity.Account;
import com.bankx.entity.Customer;
import com.bankx.enums.AccountType;
import com.bankx.enums.NotificationType;
import com.bankx.exception.CustomerAlreadyExistsException;
import com.bankx.exception.CustomerNotFoundException;
import com.bankx.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private NotificationService notificationService;

    @Value("${bankx.joining-bonus:500.00}")
    private BigDecimal joiningBonus;

    public CustomerResponse registerCustomer(CustomerRegistrationRequest request) {
        logger.info("Registering new customer with email: {}", request.getEmail());

        // Check if customer already exists
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new CustomerAlreadyExistsException("Customer with email " + request.getEmail() + " already exists");
        }

        // Create customer
        Customer customer = new Customer(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPhoneNumber()
        );
        
        // Set SA ID number if provided
        if (request.getSaIdNumber() != null && !request.getSaIdNumber().trim().isEmpty()) {
            customer.setSaIdNumber(request.getSaIdNumber());
        }

        customer = customerRepository.save(customer);
        logger.info("Customer created with ID: {}", customer.getId());

        // Create Current Account
        Account currentAccount = accountService.createAccount(customer, AccountType.CURRENT);
        logger.info("Current account created: {}", currentAccount.getAccountNumber());

        // Create Savings Account with joining bonus
        Account savingsAccount = accountService.createAccount(customer, AccountType.SAVINGS);
        accountService.creditAccount(savingsAccount.getAccountNumber(), joiningBonus, "Joining bonus");
        logger.info("Savings account created with joining bonus: {}", savingsAccount.getAccountNumber());

        // Send welcome notification
        String welcomeMessage = String.format(
                "Welcome to Bank X, %s! Your accounts have been created successfully. " +
                "Current Account: %s, Savings Account: %s. " +
                "You have received a joining bonus of R%.2f in your savings account.",
                customer.getFullName(),
                currentAccount.getAccountNumber(),
                savingsAccount.getAccountNumber(),
                joiningBonus
        );

        notificationService.sendNotification(customer.getId(), welcomeMessage, NotificationType.WELCOME);

        logger.info("Customer registration completed for: {}", customer.getEmail());
        return new CustomerResponse(customer, true);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long customerId) {
        Customer customer = customerRepository.findByIdWithAccounts(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));
        return new CustomerResponse(customer, true);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(customer -> new CustomerResponse(customer, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<Customer> getAllCustomersPaginated(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with email: " + email));
        return new CustomerResponse(customer, false);
    }

    public Customer getCustomerEntity(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));
    }

    public Customer registerCustomer(Customer customer) {
        logger.info("Registering new customer with email: {}", customer.getEmail());

        // Check if customer already exists
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new CustomerAlreadyExistsException("Customer with email " + customer.getEmail() + " already exists");
        }

        customer = customerRepository.save(customer);
        logger.info("Customer created with ID: {}", customer.getId());

        // Create Current Account
        Account currentAccount = accountService.createAccount(customer, AccountType.CURRENT);
        logger.info("Current account created: {}", currentAccount.getAccountNumber());

        // Create Savings Account with joining bonus
        Account savingsAccount = accountService.createAccount(customer, AccountType.SAVINGS);
        accountService.creditAccount(savingsAccount.getAccountNumber(), joiningBonus, "Joining bonus");
        logger.info("Savings account created with joining bonus: {}", savingsAccount.getAccountNumber());

        // Send welcome notification
        String welcomeMessage = String.format(
                "Welcome to Bank X, %s! Your accounts have been created successfully. " +
                "Current Account: %s, Savings Account: %s. " +
                "You have received a joining bonus of R%.2f in your savings account.",
                customer.getFullName(),
                currentAccount.getAccountNumber(),
                savingsAccount.getAccountNumber(),
                joiningBonus
        );

        notificationService.sendNotification(customer.getId(), welcomeMessage, NotificationType.WELCOME);

        logger.info("Customer registration completed for: {}", customer.getEmail());
        return customer;
    }

    public Customer getCustomerById(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));
    }
}