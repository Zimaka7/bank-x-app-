package com.bankx.controller;

import com.bankx.dto.BankZTransactionRequest;
import com.bankx.entity.Customer;
import com.bankx.entity.Account;
import com.bankx.entity.Account.AccountType;
import com.bankx.service.CustomerService;
import com.bankx.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
class BankZControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    private Customer testCustomer;
    private Account currentAccount;
    private Account savingsAccount;

    @BeforeEach
    void setUp() {
        // Create test customer with accounts
        testCustomer = new Customer("Test", "Customer", "test@bankz.com", "+27123456789");
        testCustomer = customerService.registerCustomer(testCustomer);
        
        List<Account> accounts = accountService.getAccountsByCustomerId(testCustomer.getId());
        currentAccount = accounts.stream()
            .filter(acc -> acc.getAccountType() == AccountType.CURRENT)
            .findFirst()
            .orElse(null);
        savingsAccount = accounts.stream()
            .filter(acc -> acc.getAccountType() == AccountType.SAVINGS)
            .findFirst()
            .orElse(null);
    }

    @Test
    void testProcessSingleTransaction_Credit() {
        // Given
        BankZTransactionRequest request = new BankZTransactionRequest();
        request.setAccountNumber(currentAccount.getAccountNumber());
        request.setAmount(new BigDecimal("100.00"));
        request.setTransactionType("CREDIT");
        request.setReference("BANKZ-CREDIT-001");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/bankz/transaction",
            request,
            String.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Transaction processed successfully"));

        // Verify balance was updated
        BigDecimal newBalance = accountService.getBalance(currentAccount.getAccountNumber());
        assertTrue(newBalance.compareTo(new BigDecimal("100.00")) > 0);
    }

    @Test
    void testProcessSingleTransaction_Debit() {
        // Given - First credit the account to have sufficient funds
        accountService.creditAccount(currentAccount.getAccountNumber(), new BigDecimal("500.00"), "Setup");
        
        BankZTransactionRequest request = new BankZTransactionRequest();
        request.setAccountNumber(currentAccount.getAccountNumber());
        request.setAmount(new BigDecimal("100.00"));
        request.setTransactionType("DEBIT");
        request.setReference("BANKZ-DEBIT-001");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/bankz/transaction",
            request,
            String.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Transaction processed successfully"));
    }

    @Test
    void testProcessSingleTransaction_InvalidAccount() {
        // Given
        BankZTransactionRequest request = new BankZTransactionRequest();
        request.setAccountNumber("INVALID-ACCOUNT");
        request.setAmount(new BigDecimal("100.00"));
        request.setTransactionType("CREDIT");
        request.setReference("BANKZ-INVALID-001");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/bankz/transaction",
            request,
            String.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testProcessBatchTransactions() {
        // Given
        BankZTransactionRequest request1 = new BankZTransactionRequest();
        request1.setAccountNumber(currentAccount.getAccountNumber());
        request1.setAmount(new BigDecimal("100.00"));
        request1.setTransactionType("CREDIT");
        request1.setReference("BANKZ-BATCH-001");

        BankZTransactionRequest request2 = new BankZTransactionRequest();
        request2.setAccountNumber(savingsAccount.getAccountNumber());
        request2.setAmount(new BigDecimal("200.00"));
        request2.setTransactionType("CREDIT");
        request2.setReference("BANKZ-BATCH-002");

        List<BankZTransactionRequest> batchRequests = Arrays.asList(request1, request2);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/bankz/batch",
            batchRequests,
            String.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Batch processed successfully"));
        assertTrue(response.getBody().contains("2 transactions"));
    }

    @Test
    void testProcessBatchTransactions_PartialFailure() {
        // Given
        BankZTransactionRequest validRequest = new BankZTransactionRequest();
        validRequest.setAccountNumber(currentAccount.getAccountNumber());
        validRequest.setAmount(new BigDecimal("100.00"));
        validRequest.setTransactionType("CREDIT");
        validRequest.setReference("BANKZ-VALID-001");

        BankZTransactionRequest invalidRequest = new BankZTransactionRequest();
        invalidRequest.setAccountNumber("INVALID-ACCOUNT");
        invalidRequest.setAmount(new BigDecimal("100.00"));
        invalidRequest.setTransactionType("CREDIT");
        invalidRequest.setReference("BANKZ-INVALID-001");

        List<BankZTransactionRequest> batchRequests = Arrays.asList(validRequest, invalidRequest);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/bankz/batch",
            batchRequests,
            String.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("1 successful"));
        assertTrue(response.getBody().contains("1 failed"));
    }

    @Test
    void testReconciliationEndpoint() {
        // Given
        BankZTransactionRequest request1 = new BankZTransactionRequest();
        request1.setAccountNumber(currentAccount.getAccountNumber());
        request1.setAmount(new BigDecimal("100.00"));
        request1.setTransactionType("CREDIT");
        request1.setReference("RECON-001");
        request1.setBatchId("BATCH-001");

        BankZTransactionRequest request2 = new BankZTransactionRequest();
        request2.setAccountNumber(savingsAccount.getAccountNumber());
        request2.setAmount(new BigDecimal("200.00"));
        request2.setTransactionType("CREDIT");
        request2.setReference("RECON-002");
        request2.setBatchId("BATCH-001");

        List<BankZTransactionRequest> reconciliationData = Arrays.asList(request1, request2);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/bankz/reconciliation",
            reconciliationData,
            String.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Reconciliation completed"));
        assertTrue(response.getBody().contains("2 transactions"));
    }

    @Test
    void testSavingsAccountInterest() {
        // Given - Credit savings account to trigger interest calculation
        BigDecimal initialBalance = accountService.getBalance(savingsAccount.getAccountNumber());
        BigDecimal creditAmount = new BigDecimal("1000.00");
        
        BankZTransactionRequest request = new BankZTransactionRequest();
        request.setAccountNumber(savingsAccount.getAccountNumber());
        request.setAmount(creditAmount);
        request.setTransactionType("CREDIT");
        request.setReference("INTEREST-TEST-001");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/bankz/transaction",
            request,
            String.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify interest was applied (0.5% of initial balance)
        BigDecimal finalBalance = accountService.getBalance(savingsAccount.getAccountNumber());
        BigDecimal expectedInterest = initialBalance.multiply(new BigDecimal("0.005"));
        BigDecimal expectedFinalBalance = initialBalance.add(creditAmount).add(expectedInterest);
        
        assertEquals(0, expectedFinalBalance.compareTo(finalBalance));
    }

    @Test
    void testTransactionFeeOnDebit() {
        // Given - First credit the account to have sufficient funds
        accountService.creditAccount(currentAccount.getAccountNumber(), new BigDecimal("1000.00"), "Setup");
        BigDecimal initialBalance = accountService.getBalance(currentAccount.getAccountNumber());
        
        BankZTransactionRequest request = new BankZTransactionRequest();
        request.setAccountNumber(currentAccount.getAccountNumber());
        request.setAmount(new BigDecimal("100.00"));
        request.setTransactionType("DEBIT");
        request.setReference("FEE-TEST-001");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/bankz/transaction",
            request,
            String.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify fee was applied (0.05% of transaction amount)
        BigDecimal finalBalance = accountService.getBalance(currentAccount.getAccountNumber());
        BigDecimal expectedFee = new BigDecimal("100.00").multiply(new BigDecimal("0.0005"));
        BigDecimal expectedFinalBalance = initialBalance.subtract(new BigDecimal("100.00")).subtract(expectedFee);
        
        assertEquals(0, expectedFinalBalance.compareTo(finalBalance));
    }
}