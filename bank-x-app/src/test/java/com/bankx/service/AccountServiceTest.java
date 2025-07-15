package com.bankx.service;

import com.bankx.entity.Account;
import com.bankx.entity.Customer;
import com.bankx.entity.Transaction;
import com.bankx.entity.Account.AccountType;
import com.bankx.entity.Transaction.TransactionType;
import com.bankx.repository.AccountRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AccountService accountService;

    private Customer testCustomer;
    private Account currentAccount;
    private Account savingsAccount;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("John", "Doe", "john.doe@example.com", "+27123456789");
        testCustomer.setId(1L);

        currentAccount = new Account();
        currentAccount.setId(1L);
        currentAccount.setAccountNumber("ACC001");
        currentAccount.setAccountType(AccountType.CURRENT);
        currentAccount.setBalance(new BigDecimal("1000.00"));
        currentAccount.setCustomer(testCustomer);

        savingsAccount = new Account();
        savingsAccount.setId(2L);
        savingsAccount.setAccountNumber("ACC002");
        savingsAccount.setAccountType(AccountType.SAVINGS);
        savingsAccount.setBalance(new BigDecimal("500.00"));
        savingsAccount.setCustomer(testCustomer);
    }

    @Test
    void testCreateAccount_Current() {
        // Given
        when(accountRepository.save(any(Account.class))).thenReturn(currentAccount);

        // When
        Account result = accountService.createAccount(testCustomer, AccountType.CURRENT);

        // Then
        assertNotNull(result);
        assertEquals(AccountType.CURRENT, result.getAccountType());
        assertEquals(testCustomer, result.getCustomer());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void testCreateAccount_Savings() {
        // Given
        when(accountRepository.save(any(Account.class))).thenReturn(savingsAccount);

        // When
        Account result = accountService.createAccount(testCustomer, AccountType.SAVINGS);

        // Then
        assertNotNull(result);
        assertEquals(AccountType.SAVINGS, result.getAccountType());
        assertEquals(testCustomer, result.getCustomer());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void testCreditAccount_Current() {
        // Given
        BigDecimal creditAmount = new BigDecimal("100.00");
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(currentAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(currentAccount);

        // When
        accountService.creditAccount("ACC001", creditAmount, "Test credit");

        // Then
        verify(accountRepository).save(currentAccount);
        verify(transactionService).createTransaction(eq(currentAccount), eq(TransactionType.CREDIT), 
                                                   eq(creditAmount), any(), eq("Test credit"));
        verify(notificationService).sendTransactionNotification(eq(testCustomer), any());
    }

    @Test
    void testCreditAccount_Savings_WithInterest() {
        // Given
        BigDecimal creditAmount = new BigDecimal("100.00");
        BigDecimal expectedInterest = new BigDecimal("2.50"); // 0.5% of 500.00 balance
        when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(savingsAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(savingsAccount);

        // When
        accountService.creditAccount("ACC002", creditAmount, "Test credit");

        // Then
        verify(accountRepository, times(2)).save(savingsAccount); // Once for credit, once for interest
        verify(transactionService).createTransaction(eq(savingsAccount), eq(TransactionType.CREDIT), 
                                                   eq(creditAmount), any(), eq("Test credit"));
        verify(transactionService).createTransaction(eq(savingsAccount), eq(TransactionType.CREDIT), 
                                                   eq(expectedInterest), any(), eq("Interest payment"));
    }

    @Test
    void testDebitAccount_SufficientFunds() {
        // Given
        BigDecimal debitAmount = new BigDecimal("100.00");
        BigDecimal expectedFee = new BigDecimal("0.05"); // 0.05% of 100.00
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(currentAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(currentAccount);

        // When
        accountService.debitAccount("ACC001", debitAmount, "Test debit");

        // Then
        verify(accountRepository, times(2)).save(currentAccount); // Once for debit, once for fee
        verify(transactionService).createTransaction(eq(currentAccount), eq(TransactionType.DEBIT), 
                                                   eq(debitAmount), any(), eq("Test debit"));
        verify(transactionService).createTransaction(eq(currentAccount), eq(TransactionType.DEBIT), 
                                                   eq(expectedFee), any(), eq("Transaction fee"));
    }

    @Test
    void testDebitAccount_InsufficientFunds() {
        // Given
        BigDecimal debitAmount = new BigDecimal("2000.00"); // More than available balance
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(currentAccount));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            accountService.debitAccount("ACC001", debitAmount, "Test debit"));
    }

    @Test
    void testTransferBetweenAccounts_Success() {
        // Given
        BigDecimal transferAmount = new BigDecimal("200.00");
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(currentAccount));
        when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(savingsAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(currentAccount, savingsAccount);

        // When
        accountService.transferBetweenAccounts("ACC001", "ACC002", transferAmount, "Test transfer");

        // Then
        verify(accountRepository, times(4)).save(any(Account.class)); // 2 for debit (amount + fee), 2 for credit (amount + interest)
        verify(transactionService, times(4)).createTransaction(any(), any(), any(), any(), any());
    }

    @Test
    void testTransferBetweenAccounts_SameAccount() {
        // When & Then
        assertThrows(RuntimeException.class, () -> 
            accountService.transferBetweenAccounts("ACC001", "ACC001", new BigDecimal("100.00"), "Test"));
    }

    @Test
    void testGetAccountByNumber_Found() {
        // Given
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(currentAccount));

        // When
        Account result = accountService.getAccountByNumber("ACC001");

        // Then
        assertNotNull(result);
        assertEquals("ACC001", result.getAccountNumber());
    }

    @Test
    void testGetAccountByNumber_NotFound() {
        // Given
        when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> accountService.getAccountByNumber("INVALID"));
    }

    @Test
    void testGetAccountsByCustomerId() {
        // Given
        List<Account> accounts = List.of(currentAccount, savingsAccount);
        when(accountRepository.findByCustomerId(1L)).thenReturn(accounts);

        // When
        List<Account> result = accountService.getAccountsByCustomerId(1L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(currentAccount));
        assertTrue(result.contains(savingsAccount));
    }

    @Test
    void testGetBalance() {
        // Given
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(currentAccount));

        // When
        BigDecimal balance = accountService.getBalance("ACC001");

        // Then
        assertEquals(new BigDecimal("1000.00"), balance);
    }
}