package com.bankx.controller;

import com.bankx.dto.ApiResponse;
import com.bankx.dto.CustomerRegistrationRequest;
import com.bankx.dto.CustomerResponse;
import com.bankx.entity.Account;
import com.bankx.entity.Customer;
import com.bankx.service.AccountService;
import com.bankx.service.CustomerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerRestController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerRestController.class);

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AccountService accountService;

    /**
     * Register a customer using Customer entity directly (for backward compatibility with tests)
     * POST /api/customers
     */
    @PostMapping
    public ResponseEntity<Customer> registerCustomer(@Valid @RequestBody Customer customer) {
        logger.info("REST API: Customer registration with email: {}", customer.getEmail());
        
        try {
            Customer registeredCustomer = customerService.registerCustomer(customer);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredCustomer);
            
        } catch (Exception e) {
            logger.error("Failed to register customer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Register a new customer using DTO
     * POST /api/customers/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CustomerResponse>> registerCustomerWithDTO(
            @Valid @RequestBody CustomerRegistrationRequest request) {
        
        logger.info("REST API: Registering new customer with email: {}", request.getEmail());
        
        try {
            CustomerResponse customerResponse = customerService.registerCustomer(request);
            
            ApiResponse<CustomerResponse> response = ApiResponse.success(
                "Customer registered successfully", 
                customerResponse
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Failed to register customer: {}", e.getMessage());
            
            ApiResponse<CustomerResponse> response = ApiResponse.error(
                "Registration failed: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Get all customers
     * GET /api/customers
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        logger.info("REST API: Getting all customers");
        
        try {
            List<CustomerResponse> customers = customerService.getAllCustomers();
            
            ApiResponse<List<CustomerResponse>> response = ApiResponse.success(
                "Customers retrieved successfully", 
                customers
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get customers: {}", e.getMessage());
            
            ApiResponse<List<CustomerResponse>> response = ApiResponse.error(
                "Failed to retrieve customers: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get customer by ID
     * GET /api/customers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable Long id) {
        logger.info("REST API: Getting customer with ID: {}", id);
        
        try {
            CustomerResponse customer = customerService.getCustomer(id);
            
            ApiResponse<CustomerResponse> response = ApiResponse.success(
                "Customer retrieved successfully", 
                customer
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get customer {}: {}", id, e.getMessage());
            
            ApiResponse<CustomerResponse> response = ApiResponse.error(
                "Customer not found: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Get customer accounts
     * GET /api/customers/{id}/accounts
     */
    @GetMapping("/{id}/accounts")
    public ResponseEntity<ApiResponse<List<Account>>> getCustomerAccounts(@PathVariable Long id) {
        logger.info("REST API: Getting accounts for customer ID: {}", id);
        
        try {
            // Verify customer exists
            customerService.getCustomer(id);
            
            List<Account> accounts = accountService.getAccountsByCustomerId(id);
            
            ApiResponse<List<Account>> response = ApiResponse.success(
                "Customer accounts retrieved successfully", 
                accounts
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get accounts for customer {}: {}", id, e.getMessage());
            
            ApiResponse<List<Account>> response = ApiResponse.error(
                "Failed to retrieve customer accounts: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Get customer by email
     * GET /api/customers/email/{email}
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByEmail(@PathVariable String email) {
        logger.info("REST API: Getting customer with email: {}", email);
        
        try {
            CustomerResponse customer = customerService.getCustomerByEmail(email);
            
            ApiResponse<CustomerResponse> response = ApiResponse.success(
                "Customer retrieved successfully", 
                customer
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get customer with email {}: {}", email, e.getMessage());
            
            ApiResponse<CustomerResponse> response = ApiResponse.error(
                "Customer not found: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}