package com.bankx.service;

import com.bankx.dto.BankZTransactionRequest;
import com.bankx.entity.Account;
import com.bankx.entity.BankZTransaction;
import com.bankx.entity.Customer;
import com.bankx.enums.NotificationType;
import com.bankx.enums.TransactionType;
import com.bankx.exception.AccountNotFoundException;
import com.bankx.repository.AccountRepository;
import com.bankx.repository.BankZTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class BankZIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(BankZIntegrationService.class);

    @Autowired
    private BankZTransactionRepository bankZTransactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private NotificationService notificationService;

    public void processSingleTransaction(BankZTransactionRequest request) {
        logger.info("Processing single Bank Z transaction for account: {}", request.getAccountNumber());

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

        // Record Bank Z transaction
        BankZTransaction bankZTransaction = new BankZTransaction(
                account.getCustomer(),
                request.getAccountNumber(),
                request.getAmount(),
                request.getTransactionType(),
                request.getReference()
        );
        bankZTransactionRepository.save(bankZTransaction);

        // Process the actual transaction
        processAccountTransaction(account, request);

        logger.info("Single Bank Z transaction processed successfully: {}", request.getReference());
    }

    public void processBatchTransactions(List<BankZTransactionRequest> requests) {
        String batchId = UUID.randomUUID().toString();
        logger.info("Processing Bank Z batch transactions with batch ID: {}", batchId);

        for (BankZTransactionRequest request : requests) {
            try {
                Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                        .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

                // Record Bank Z transaction with batch ID
                BankZTransaction bankZTransaction = new BankZTransaction(
                        account.getCustomer(),
                        request.getAccountNumber(),
                        request.getAmount(),
                        request.getTransactionType(),
                        request.getReference(),
                        batchId
                );
                bankZTransactionRepository.save(bankZTransaction);

                // Process the actual transaction
                processAccountTransaction(account, request);

                logger.info("Batch transaction processed: {} for account: {}", 
                        request.getReference(), request.getAccountNumber());

            } catch (Exception e) {
                logger.error("Failed to process batch transaction: {} for account: {}", 
                        request.getReference(), request.getAccountNumber(), e);
                // In production, you might want to implement retry logic or dead letter queue
            }
        }

        logger.info("Bank Z batch transactions processing completed for batch: {}", batchId);
    }

    public void processReconciliationTransactions(List<BankZTransactionRequest> requests) {
        String reconciliationBatchId = "RECON-" + UUID.randomUUID().toString();
        logger.info("Processing Bank Z reconciliation transactions with batch ID: {}", reconciliationBatchId);

        for (BankZTransactionRequest request : requests) {
            try {
                Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                        .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

                // Record Bank Z reconciliation transaction
                BankZTransaction bankZTransaction = new BankZTransaction(
                        account.getCustomer(),
                        request.getAccountNumber(),
                        request.getAmount(),
                        request.getTransactionType(),
                        request.getReference(),
                        reconciliationBatchId
                );
                bankZTransactionRepository.save(bankZTransaction);

                logger.info("Reconciliation transaction recorded: {} for account: {}", 
                        request.getReference(), request.getAccountNumber());

            } catch (Exception e) {
                logger.error("Failed to record reconciliation transaction: {} for account: {}", 
                        request.getReference(), request.getAccountNumber(), e);
            }
        }

        logger.info("Bank Z reconciliation transactions processing completed for batch: {}", reconciliationBatchId);
    }

    @Transactional(readOnly = true)
    public List<BankZTransaction> getCustomerBankZTransactions(Long customerId) {
        return bankZTransactionRepository.findByCustomerIdOrderByProcessedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<BankZTransaction> getAccountBankZTransactions(String accountNumber) {
        return bankZTransactionRepository.findByAccountNumberOrderByProcessedAtDesc(accountNumber);
    }

    @Transactional(readOnly = true)
    public List<BankZTransaction> getBatchTransactions(String batchId) {
        return bankZTransactionRepository.findByBatchIdOrderByProcessedAtDesc(batchId);
    }

    @Transactional(readOnly = true)
    public List<BankZTransaction> getTransactionsByReference(String reference) {
        return bankZTransactionRepository.findByReference(reference);
    }

    // Admin-specific methods
    @Transactional(readOnly = true)
    public Page<BankZTransaction> getAllBankZTransactionsPaginated(Pageable pageable) {
        return bankZTransactionRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBankZTransactionStats() {
        Map<String, Object> stats = new HashMap<>();
        
        List<BankZTransaction> allTransactions = bankZTransactionRepository.findAll();
        
        long totalTransactions = allTransactions.size();
        long creditTransactions = allTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.CREDIT)
                .count();
        long debitTransactions = allTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)
                .count();
        
        BigDecimal totalCreditAmount = allTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.CREDIT)
                .map(BankZTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalDebitAmount = allTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)
                .map(BankZTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Count batch transactions
        long batchTransactions = allTransactions.stream()
                .filter(t -> t.getBatchId() != null && !t.getBatchId().isEmpty())
                .count();
        
        // Count reconciliation transactions
        long reconciliationTransactions = allTransactions.stream()
                .filter(t -> t.getBatchId() != null && t.getBatchId().startsWith("RECON-"))
                .count();
        
        stats.put("totalTransactions", totalTransactions);
        stats.put("creditTransactions", creditTransactions);
        stats.put("debitTransactions", debitTransactions);
        stats.put("totalCreditAmount", totalCreditAmount);
        stats.put("totalDebitAmount", totalDebitAmount);
        stats.put("batchTransactions", batchTransactions);
        stats.put("reconciliationTransactions", reconciliationTransactions);
        stats.put("lastUpdated", LocalDateTime.now());
        
        return stats;
    }

    @Transactional(readOnly = true)
    public List<BankZTransaction> getFailedBankZTransactions() {
        // For now, return empty list as we don't have a failed status field
        // In a real implementation, you would have a status field to track failed transactions
        return List.of();
    }

    @Transactional
    public void performManualReconciliation(Long transactionId) {
        logger.info("Performing manual reconciliation for Bank Z transaction: {}", transactionId);
        
        BankZTransaction transaction = bankZTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Bank Z transaction not found: " + transactionId));
        
        // In a real implementation, you would perform the actual reconciliation logic here
        // For now, we'll just log the action
        logger.info("Manual reconciliation completed for transaction: {} with reference: {}", 
                   transactionId, transaction.getReference());
    }

    private void processAccountTransaction(Account account, BankZTransactionRequest request) {
        String description = String.format("Bank Z transaction - %s", request.getReference());

        if (request.getTransactionType() == TransactionType.CREDIT) {
            accountService.creditAccount(request.getAccountNumber(), request.getAmount(), description);
        } else if (request.getTransactionType() == TransactionType.DEBIT) {
            accountService.debitAccount(request.getAccountNumber(), request.getAmount(), description);
        }

        // Send specific notification for Bank Z transactions
        String message = String.format(
                "Bank Z %s transaction processed: R%.2f. Reference: %s. Account: %s",
                request.getTransactionType().getDisplayName().toLowerCase(),
                request.getAmount(),
                request.getReference(),
                request.getAccountNumber()
        );

        notificationService.sendNotification(account.getCustomer().getId(), message, NotificationType.TRANSACTION);
    }
}