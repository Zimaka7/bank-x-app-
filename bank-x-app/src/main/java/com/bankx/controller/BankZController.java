package com.bankx.controller;

import com.bankx.dto.ApiResponse;
import com.bankx.dto.BankZTransactionRequest;
import com.bankx.entity.BankZTransaction;
import com.bankx.service.BankZIntegrationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bankz")
public class BankZController {

    private static final Logger logger = LoggerFactory.getLogger(BankZController.class);

    @Autowired
    private BankZIntegrationService bankZIntegrationService;

    @PostMapping("/transaction")
    public ResponseEntity<ApiResponse<String>> processSingleTransaction(
            @Valid @RequestBody BankZTransactionRequest request) {
        
        logger.info("Processing single Bank Z transaction for account: {} amount: {}", 
                request.getAccountNumber(), request.getAmount());
        
        bankZIntegrationService.processSingleTransaction(request);
        
        return ResponseEntity.ok(ApiResponse.success("Bank Z transaction processed successfully"));
    }

    @PostMapping("/transactions/batch")
    public ResponseEntity<ApiResponse<String>> processBatchTransactions(
            @Valid @RequestBody List<BankZTransactionRequest> requests) {
        
        logger.info("Processing Bank Z batch transactions, count: {}", requests.size());
        
        bankZIntegrationService.processBatchTransactions(requests);
        
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Bank Z batch transactions processed successfully (%d transactions)", requests.size())));
    }

    @PostMapping("/reconciliation")
    public ResponseEntity<ApiResponse<String>> processReconciliationTransactions(
            @Valid @RequestBody List<BankZTransactionRequest> requests) {
        
        logger.info("Processing Bank Z reconciliation transactions, count: {}", requests.size());
        
        bankZIntegrationService.processReconciliationTransactions(requests);
        
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Bank Z reconciliation transactions processed successfully (%d transactions)", requests.size())));
    }

    @GetMapping("/transactions/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<BankZTransaction>>> getCustomerBankZTransactions(
            @PathVariable Long customerId) {
        
        logger.info("Retrieving Bank Z transactions for customer: {}", customerId);
        
        List<BankZTransaction> transactions = bankZIntegrationService.getCustomerBankZTransactions(customerId);
        
        return ResponseEntity.ok(ApiResponse.success("Bank Z transactions retrieved successfully", transactions));
    }

    @GetMapping("/transactions/account/{accountNumber}")
    public ResponseEntity<ApiResponse<List<BankZTransaction>>> getAccountBankZTransactions(
            @PathVariable String accountNumber) {
        
        logger.info("Retrieving Bank Z transactions for account: {}", accountNumber);
        
        List<BankZTransaction> transactions = bankZIntegrationService.getAccountBankZTransactions(accountNumber);
        
        return ResponseEntity.ok(ApiResponse.success("Bank Z transactions retrieved successfully", transactions));
    }

    @GetMapping("/transactions/batch/{batchId}")
    public ResponseEntity<ApiResponse<List<BankZTransaction>>> getBatchTransactions(@PathVariable String batchId) {
        logger.info("Retrieving Bank Z batch transactions for batch: {}", batchId);
        
        List<BankZTransaction> transactions = bankZIntegrationService.getBatchTransactions(batchId);
        
        return ResponseEntity.ok(ApiResponse.success("Bank Z batch transactions retrieved successfully", transactions));
    }

    @GetMapping("/status/{reference}")
    public ResponseEntity<ApiResponse<List<BankZTransaction>>> getTransactionStatus(@PathVariable String reference) {
        logger.info("Retrieving Bank Z transaction status for reference: {}", reference);
        
        List<BankZTransaction> transactions = bankZIntegrationService.getTransactionsByReference(reference);
        
        return ResponseEntity.ok(ApiResponse.success("Bank Z transaction status retrieved successfully", transactions));
    }
}