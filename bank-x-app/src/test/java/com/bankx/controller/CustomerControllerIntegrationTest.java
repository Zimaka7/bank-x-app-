package com.bankx.controller;

import com.bankx.entity.Customer;
import com.bankx.entity.Account;
import com.bankx.entity.Account.AccountType;
import com.bankx.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
class CustomerControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRegisterCustomer_Success() {
        // Given
        Customer customer = new Customer("John", "Doe", "john.doe@test.com", "+27123456789");

        // When
        ResponseEntity<Customer> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/customers",
            customer,
            Customer.class
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("John", response.getBody().getFirstName());
        assertEquals("Doe", response.getBody().getLastName());
        assertEquals("john.doe@test.com", response.getBody().getEmail());
    }

    @Test
    void testRegisterCustomer_InvalidEmail() {
        // Given
        Customer customer = new Customer("John", "Doe", "invalid-email", "+27123456789");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/customers",
            customer,
            String.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetAllCustomers() {
        // Given - Register a customer first
        Customer customer = new Customer("Jane", "Smith", "jane.smith@test.com", "+27987654321");
        customerService.registerCustomer(customer);

        // When
        ResponseEntity<Customer[]> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/customers",
            Customer[].class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }

    @Test
    void testGetCustomerById_Found() {
        // Given
        Customer customer = new Customer("Bob", "Johnson", "bob.johnson@test.com", "+27555666777");
        Customer savedCustomer = customerService.registerCustomer(customer);

        // When
        ResponseEntity<Customer> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/customers/" + savedCustomer.getId(),
            Customer.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(savedCustomer.getId(), response.getBody().getId());
        assertEquals("Bob", response.getBody().getFirstName());
    }

    @Test
    void testGetCustomerById_NotFound() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/customers/99999",
            String.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testCustomerRegistration_CreatesAccountsWithBonus() {
        // Given
        Customer customer = new Customer("Alice", "Brown", "alice.brown@test.com", "+27111222333");

        // When
        ResponseEntity<Customer> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/customers",
            customer,
            Customer.class
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Customer savedCustomer = response.getBody();
        assertNotNull(savedCustomer);

        // Verify accounts were created
        ResponseEntity<Account[]> accountsResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/customers/" + savedCustomer.getId() + "/accounts",
            Account[].class
        );

        assertEquals(HttpStatus.OK, accountsResponse.getStatusCode());
        Account[] accounts = accountsResponse.getBody();
        assertNotNull(accounts);
        assertEquals(2, accounts.length);

        // Verify account types
        boolean hasCurrentAccount = false;
        boolean hasSavingsAccount = false;
        boolean hasBonusInSavings = false;

        for (Account account : accounts) {
            if (account.getAccountType() == AccountType.CURRENT) {
                hasCurrentAccount = true;
            } else if (account.getAccountType() == AccountType.SAVINGS) {
                hasSavingsAccount = true;
                // Check if savings account has the R500 bonus
                if (account.getBalance().compareTo(new BigDecimal("500.00")) >= 0) {
                    hasBonusInSavings = true;
                }
            }
        }

        assertTrue(hasCurrentAccount, "Customer should have a Current account");
        assertTrue(hasSavingsAccount, "Customer should have a Savings account");
        assertTrue(hasBonusInSavings, "Savings account should have R500 bonus");
    }
}