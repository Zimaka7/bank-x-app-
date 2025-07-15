package com.bankz.service;

import com.bankz.entity.BankZAccount;
import com.bankz.entity.BankZCustomer;
import com.bankz.entity.BankZTransaction;
import com.bankz.repository.BankZAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BankZAccountService {
    
    @Autowired
    private BankZAccountRepository accountRepository;
    
    @Autowired
    private BankZTransactionService transactionService;
    
    public BankZAccount createAccount(BankZCustomer customer, String accountNumber, BankZAccount.AccountType accountType) {
        BankZAccount account = new BankZAccount(customer, accountNumber, accountType);
        return accountRepository.save(account);
    }
    
    public List<BankZAccount> getAllAccounts() {
        List<BankZAccount> accounts = accountRepository.findAll();
        System.out.println("DEBUG: AccountService.getAllAccounts() found " + accounts.size() + " accounts");
        
        // Ensure customer relationships are loaded
        for (BankZAccount account : accounts) {
            if (account.getCustomer() != null) {
                // Force loading of customer data
                String customerName = account.getCustomer().getFirstName();
                System.out.println("DEBUG: Account " + account.getAccountNumber() + " belongs to " + customerName);
            } else {
                System.out.println("WARNING: Account " + account.getAccountNumber() + " has no customer!");
            }
        }
        
        return accounts;
    }
    
    public Optional<BankZAccount> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }
    
    public List<BankZAccount> getCustomerAccounts(Long customerId) {
        return accountRepository.findByCustomerId(customerId);
    }
    
    public long countActiveAccounts() {
        return accountRepository.countActiveAccounts();
    }
    
    public BankZTransaction creditAccount(String accountNumber, BigDecimal amount, String description, String reference) {
        BankZAccount account = getAccountByNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
        
        BigDecimal balanceBefore = account.getBalance();
        account.setBalance(account.getBalance().add(amount));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);
        
        // Create transaction record
        BankZTransaction transaction = transactionService.createTransaction(
                account, BankZTransaction.TransactionType.CREDIT, amount, description, reference, balanceBefore, account.getBalance());
        
        return transaction;
    }
    
    public BankZTransaction debitAccount(String accountNumber, BigDecimal amount, String description, String reference) {
        BankZAccount account = getAccountByNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
        
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        
        BigDecimal balanceBefore = account.getBalance();
        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);
        
        // Create transaction record
        BankZTransaction transaction = transactionService.createTransaction(
                account, BankZTransaction.TransactionType.DEBIT, amount, description, reference, balanceBefore, account.getBalance());
        
        return transaction;
    }
}