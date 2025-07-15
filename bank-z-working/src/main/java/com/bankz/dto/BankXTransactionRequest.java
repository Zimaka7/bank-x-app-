package com.bankz.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BankXTransactionRequest {
    
    private String customerId;
    private String transactionType;
    private String accountType;
    private BigDecimal amount;
    private String reference;
    private LocalDateTime timestamp;
    
    // Constructors
    public BankXTransactionRequest() {}
    
    public BankXTransactionRequest(String customerId, String transactionType, String accountType, 
                                 BigDecimal amount, String reference) {
        this.customerId = customerId;
        this.transactionType = transactionType;
        this.accountType = accountType;
        this.amount = amount;
        this.reference = reference;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}