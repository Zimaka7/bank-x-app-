package com.bankx.service;

import com.bankx.dto.PaymentRequest;
import com.bankx.dto.TransactionResponse;
import com.bankx.entity.Account;
import com.bankx.entity.Transaction;
import com.bankx.enums.AccountType;
import com.bankx.enums.NotificationType;
import com.bankx.enums.TransactionType;
import com.bankx.exception.AccountNotFoundException;
import com.bankx.exception.InsufficientFundsException;
import com.bankx.exception.InvalidTransactionException;
import com.bankx.exception.TransactionNotFoundException;
import com.bankx.repository.AccountRepository;
import com.bankx.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private NotificationService notificationService;

    @Value("${bankx.savings-interest-rate:0.005}")
    private BigDecimal savingsInterestRate;

    @Value("${bankx.transaction-fee-rate:0.0005}")
    private BigDecimal transactionFeeRate;

    public Transaction createTransaction(Account account, String transactionId, BigDecimal amount, 
                                      TransactionType transactionType, String description) {
        Transaction transaction = new Transaction(account, transactionId, amount, transactionType, description);
        return transaction;
    }

    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public void processPayment(PaymentRequest request) {
        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("From account not found: " + request.getFromAccountNumber()));
        
        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("To account not found: " + request.getToAccountNumber()));

        // Validate that only Current accounts can make payments to other accounts
        if (fromAccount.getAccountType() != AccountType.CURRENT) {
            throw new InvalidTransactionException("Only Current accounts can make payments to other accounts");
        }

        BigDecimal amount = request.getAmount();
        String description = request.getDescription() != null ? request.getDescription() : "Payment";
        String reference = request.getReference();

        // Calculate transaction fee
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
            
            if (interest.compareTo(BigDecimal.ZERO) > 0) {
                logger.info("Applied interest of R{} to savings account: {}", interest, toAccount.getAccountNumber());
            }
        }
        
        toAccount.credit(creditAmount);

        // Save accounts
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Record transactions
        String paymentId = UUID.randomUUID().toString();
        
        // Debit transaction
        Transaction debitTransaction = createTransaction(
                fromAccount, paymentId + "-DEBIT", amount, TransactionType.DEBIT, 
                description + " to " + request.getToAccountNumber()
        );
        debitTransaction.setReferenceNumber(reference);
        debitTransaction.setBalanceAfter(fromAccount.getBalance());
        saveTransaction(debitTransaction);

        // Credit transaction
        Transaction creditTransaction = createTransaction(
                toAccount, paymentId + "-CREDIT", amount, TransactionType.CREDIT, 
                description + " from " + request.getFromAccountNumber()
        );
        creditTransaction.setReferenceNumber(reference);
        creditTransaction.setBalanceAfter(toAccount.getBalance());
        saveTransaction(creditTransaction);

        // Fee transaction
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            Transaction feeTransaction = createTransaction(
                    fromAccount, paymentId + "-FEE", fee, TransactionType.DEBIT, "Payment fee"
            );
            feeTransaction.setReferenceNumber(reference);
            feeTransaction.setBalanceAfter(fromAccount.getBalance());
            saveTransaction(feeTransaction);
        }

        // Send notifications
        String debitMessage = String.format(
                "Payment of R%.2f to %s completed. Reference: %s. Fee: R%.2f. New balance: R%.2f",
                amount, request.getToAccountNumber(), reference, fee, fromAccount.getBalance()
        );
        
        String creditMessage = String.format(
                "Payment of R%.2f received from %s. Reference: %s. New balance: R%.2f",
                amount, request.getFromAccountNumber(), reference, toAccount.getBalance()
        );

        notificationService.sendNotification(fromAccount.getCustomer().getId(), debitMessage, NotificationType.PAYMENT);
        notificationService.sendNotification(toAccount.getCustomer().getId(), creditMessage, NotificationType.PAYMENT);

        logger.info("Payment completed: R{} from {} to {}, reference: {}", 
                amount, request.getFromAccountNumber(), request.getToAccountNumber(), reference);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionId));
        return new TransactionResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAccountTransactions(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId());
        return transactions.stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAccountTransactions(String accountNumber, Pageable pageable) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        
        Page<Transaction> transactions = transactionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId(), pageable);
        return transactions.map(TransactionResponse::new);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getCustomerTransactions(Long customerId) {
        List<Transaction> transactions = transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        return transactions.stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByCustomerId(Long customerId, int limit) {
        List<Transaction> transactions = transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        return transactions.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Transaction> getRecentTransactions(int limit) {
        return transactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getAllTransactionsPaginated(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }
}