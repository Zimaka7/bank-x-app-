package com.bankx.service;

import com.bankx.dto.AccountResponse;
import com.bankx.dto.TransferRequest;
import com.bankx.entity.Account;
import com.bankx.entity.Customer;
import com.bankx.entity.Transaction;
import com.bankx.enums.AccountType;
import com.bankx.enums.NotificationType;
import com.bankx.enums.TransactionType;
import com.bankx.exception.AccountNotFoundException;
import com.bankx.exception.InsufficientFundsException;
import com.bankx.exception.InvalidTransactionException;
import com.bankx.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private NotificationService notificationService;

    @Value("${bankx.savings-interest-rate:0.005}")
    private BigDecimal savingsInterestRate;

    @Value("${bankx.transaction-fee-rate:0.0005}")
    private BigDecimal transactionFeeRate;

    public Account createAccount(Customer customer, AccountType accountType) {
        String accountNumber = generateAccountNumber(accountType);
        
        Account account = new Account(customer, accountNumber, accountType);
        account = accountRepository.save(account);
        
        logger.info("Created {} account: {} for customer: {}", 
                accountType, accountNumber, customer.getId());
        
        return account;
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return new AccountResponse(account);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return account.getBalance();
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getCustomerAccounts(Long customerId) {
        List<Account> accounts = accountRepository.findByCustomerId(customerId);
        return accounts.stream()
                .map(AccountResponse::new)
                .collect(Collectors.toList());
    }

    public void creditAccount(String accountNumber, BigDecimal amount, String description) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        BigDecimal originalAmount = amount;
        
        // Apply interest for savings account credits
        if (account.getAccountType() == AccountType.SAVINGS) {
            BigDecimal interest = account.getBalance().multiply(savingsInterestRate)
                    .setScale(2, RoundingMode.HALF_UP);
            amount = amount.add(interest);
            
            if (interest.compareTo(BigDecimal.ZERO) > 0) {
                logger.info("Applied interest of R{} to savings account: {}", interest, accountNumber);
            }
        }

        account.credit(amount);
        account = accountRepository.save(account);

        // Record transaction
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = transactionService.createTransaction(
                account, transactionId, originalAmount, TransactionType.CREDIT, description
        );
        transaction.setBalanceAfter(account.getBalance());
        transactionService.saveTransaction(transaction);

        // Send notification
        String message = String.format(
                "Your %s account %s has been credited with R%.2f. New balance: R%.2f",
                account.getAccountType().getDisplayName(),
                accountNumber,
                originalAmount,
                account.getBalance()
        );
        notificationService.sendNotification(account.getCustomer().getId(), message, NotificationType.ACCOUNT_CREDIT);

        logger.info("Credited R{} to account: {}, new balance: R{}", 
                originalAmount, accountNumber, account.getBalance());
    }

    public void debitAccount(String accountNumber, BigDecimal amount, String description) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        // Calculate transaction fee
        BigDecimal fee = amount.multiply(transactionFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = amount.add(fee);

        if (!account.hasSufficientFunds(totalAmount)) {
            throw new InsufficientFundsException("Insufficient funds in account: " + accountNumber);
        }

        account.debit(totalAmount);
        account = accountRepository.save(account);

        // Record main transaction
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = transactionService.createTransaction(
                account, transactionId, amount, TransactionType.DEBIT, description
        );
        transaction.setBalanceAfter(account.getBalance());
        transactionService.saveTransaction(transaction);

        // Record fee transaction if applicable
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            String feeTransactionId = UUID.randomUUID().toString();
            Transaction feeTransaction = transactionService.createTransaction(
                    account, feeTransactionId, fee, TransactionType.DEBIT, "Transaction fee"
            );
            feeTransaction.setBalanceAfter(account.getBalance());
            transactionService.saveTransaction(feeTransaction);
        }

        // Send notification
        String message = String.format(
                "Your %s account %s has been debited with R%.2f (including fee: R%.2f). New balance: R%.2f",
                account.getAccountType().getDisplayName(),
                accountNumber,
                amount,
                fee,
                account.getBalance()
        );
        notificationService.sendNotification(account.getCustomer().getId(), message, NotificationType.ACCOUNT_DEBIT);

        logger.info("Debited R{} (+ R{} fee) from account: {}, new balance: R{}", 
                amount, fee, accountNumber, account.getBalance());
    }

    public void transferBetweenAccounts(TransferRequest request) {
        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("From account not found: " + request.getFromAccountNumber()));
        
        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("To account not found: " + request.getToAccountNumber()));

        // Validate that accounts belong to the same customer
        if (!fromAccount.getCustomer().getId().equals(toAccount.getCustomer().getId())) {
            throw new InvalidTransactionException("Transfer only allowed between accounts of the same customer");
        }

        BigDecimal amount = request.getAmount();
        String description = request.getDescription() != null ? request.getDescription() : "Account transfer";

        // Calculate transaction fee for debit
        BigDecimal fee = amount.multiply(transactionFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDebitAmount = amount.add(fee);

        if (!fromAccount.hasSufficientFunds(totalDebitAmount)) {
            throw new InsufficientFundsException("Insufficient funds in account: " + request.getFromAccountNumber());
        }

        // Debit from source account
        fromAccount.debit(totalDebitAmount);
        
        // Credit to destination account (with interest if savings)
        BigDecimal creditAmount = amount;
        if (toAccount.getAccountType() == AccountType.SAVINGS) {
            BigDecimal interest = toAccount.getBalance().multiply(savingsInterestRate)
                    .setScale(2, RoundingMode.HALF_UP);
            creditAmount = creditAmount.add(interest);
        }
        
        toAccount.credit(creditAmount);

        // Save accounts
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Record transactions
        String transferId = UUID.randomUUID().toString();
        
        // Debit transaction
        Transaction debitTransaction = transactionService.createTransaction(
                fromAccount, transferId + "-DEBIT", amount, TransactionType.TRANSFER, 
                description + " to " + request.getToAccountNumber()
        );
        debitTransaction.setBalanceAfter(fromAccount.getBalance());
        transactionService.saveTransaction(debitTransaction);

        // Credit transaction
        Transaction creditTransaction = transactionService.createTransaction(
                toAccount, transferId + "-CREDIT", amount, TransactionType.TRANSFER, 
                description + " from " + request.getFromAccountNumber()
        );
        creditTransaction.setBalanceAfter(toAccount.getBalance());
        transactionService.saveTransaction(creditTransaction);

        // Fee transaction
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            Transaction feeTransaction = transactionService.createTransaction(
                    fromAccount, transferId + "-FEE", fee, TransactionType.DEBIT, "Transfer fee"
            );
            feeTransaction.setBalanceAfter(fromAccount.getBalance());
            transactionService.saveTransaction(feeTransaction);
        }

        // Send notifications
        String debitMessage = String.format(
                "Transfer of R%.2f from %s to %s completed. Fee: R%.2f. New balance: R%.2f",
                amount, request.getFromAccountNumber(), request.getToAccountNumber(), 
                fee, fromAccount.getBalance()
        );
        
        String creditMessage = String.format(
                "Transfer of R%.2f received from %s to %s. New balance: R%.2f",
                amount, request.getFromAccountNumber(), request.getToAccountNumber(), 
                toAccount.getBalance()
        );

        notificationService.sendNotification(fromAccount.getCustomer().getId(), debitMessage, NotificationType.TRANSFER);
        notificationService.sendNotification(toAccount.getCustomer().getId(), creditMessage, NotificationType.TRANSFER);

        logger.info("Transfer completed: R{} from {} to {}", amount, 
                request.getFromAccountNumber(), request.getToAccountNumber());
    }

    public Account getAccountEntity(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByCustomerId(Long customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountNumber) {
        Account account = getAccountByNumber(accountNumber);
        return account.getBalance();
    }

    public void transferBetweenAccounts(String fromAccountNumber, String toAccountNumber, 
                                       BigDecimal amount, String description) {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber(fromAccountNumber);
        request.setToAccountNumber(toAccountNumber);
        request.setAmount(amount);
        request.setDescription(description);
        transferBetweenAccounts(request);
    }

    private String generateAccountNumber(AccountType accountType) {
        String prefix = accountType == AccountType.CURRENT ? "CUR" : "SAV";
        String timestamp = String.valueOf(System.currentTimeMillis());
        return prefix + timestamp.substring(timestamp.length() - 10);
    }
}