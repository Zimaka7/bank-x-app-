package com.bankx.service;

import com.bankx.entity.Customer;
import com.bankx.entity.Account;
import com.bankx.entity.Account.AccountType;
import com.bankx.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CustomerService customerService;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("John", "Doe", "john.doe@example.com", "+27123456789");
        testCustomer.setId(1L);
    }

    @Test
    void testRegisterCustomer_Success() {
        // Given
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        when(accountService.createAccount(any(Customer.class), eq(AccountType.CURRENT)))
            .thenReturn(new Account());
        when(accountService.createAccount(any(Customer.class), eq(AccountType.SAVINGS)))
            .thenReturn(new Account());

        // When
        Customer result = customerService.registerCustomer(testCustomer);

        // Then
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john.doe@example.com", result.getEmail());
        
        verify(customerRepository).save(testCustomer);
        verify(accountService).createAccount(testCustomer, AccountType.CURRENT);
        verify(accountService).createAccount(testCustomer, AccountType.SAVINGS);
        verify(accountService).creditAccount(any(), eq(new BigDecimal("500.00")), eq("Welcome bonus"));
        verify(notificationService).sendWelcomeNotification(testCustomer);
    }

    @Test
    void testGetCustomerById_Found() {
        // Given
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        // When
        Customer result = customerService.getCustomerById(1L);

        // Then
        assertNotNull(result);
        assertEquals(testCustomer.getId(), result.getId());
        assertEquals(testCustomer.getEmail(), result.getEmail());
    }

    @Test
    void testGetCustomerById_NotFound() {
        // Given
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> customerService.getCustomerById(1L));
    }

    @Test
    void testGetCustomerByEmail_Found() {
        // Given
        when(customerRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testCustomer));

        // When
        Optional<Customer> result = customerService.getCustomerByEmail("john.doe@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testCustomer.getEmail(), result.get().getEmail());
    }

    @Test
    void testGetAllCustomers() {
        // Given
        List<Customer> customers = List.of(testCustomer);
        when(customerRepository.findAll()).thenReturn(customers);

        // When
        List<Customer> result = customerService.getAllCustomers();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCustomer.getId(), result.get(0).getId());
    }

    @Test
    void testUpdateCustomer() {
        // Given
        testCustomer.setFirstName("Jane");
        when(customerRepository.save(testCustomer)).thenReturn(testCustomer);

        // When
        Customer result = customerService.updateCustomer(testCustomer);

        // Then
        assertNotNull(result);
        assertEquals("Jane", result.getFirstName());
        verify(customerRepository).save(testCustomer);
    }

    @Test
    void testDeleteCustomer() {
        // When
        customerService.deleteCustomer(1L);

        // Then
        verify(customerRepository).deleteById(1L);
    }
}