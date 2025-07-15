package com.bankz.service;

import com.bankz.entity.BankZAccount;
import com.bankz.entity.BankZTransaction;
import com.bankz.repository.BankZTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class BankZTransactionService {
    
    @Autowired
    private BankZTransactionRepository transactionRepository;
    
    public BankZTransaction createTransaction(BankZAccount account, BankZTransaction.TransactionType transactionType, 
                                           BigDecimal amount, String description, String reference,
                                           BigDecimal balanceBefore, BigDecimal balanceAfter) {
        String transactionId = generateTransactionId();
        
        BankZTransaction transaction = new BankZTransaction(transactionId, transactionType, amount, account);
        transaction.setDescription(description);
        transaction.setReference(reference);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setStatus(BankZTransaction.TransactionStatus.COMPLETED);
        
        return transactionRepository.save(transaction);
    }
    
    public Optional<BankZTransaction> getTransactionById(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }
    
    public List<BankZTransaction> getAccountTransactions(BankZAccount account) {
        return transactionRepository.findByAccountOrderByCreatedAtDesc(account);
    }
    
    public List<BankZTransaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
    
    public List<BankZTransaction> getTransactionsByReference(String reference) {
        return transactionRepository.findByReference(reference);
    }
    
    private String generateTransactionId() {
        return "BZ-TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}