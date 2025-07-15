package com.bankx.controller;

import com.bankx.dto.ApiResponse;
import com.bankx.dto.PaymentRequest;
import com.bankx.dto.TransactionResponse;
import com.bankx.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/payment")
    public ResponseEntity<ApiResponse<String>> processPayment(@Valid @RequestBody PaymentRequest request) {
        logger.info("Processing payment from {} to {} amount: {}", 
                request.getFromAccountNumber(), request.getToAccountNumber(), request.getAmount());
        
        transactionService.processPayment(request);
        
        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully"));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@PathVariable String transactionId) {
        logger.info("Retrieving transaction: {}", transactionId);
        
        TransactionResponse transaction = transactionService.getTransaction(transactionId);
        
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved successfully", transaction));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAccountTransactions(
            @PathVariable String accountNumber) {
        
        logger.info("Retrieving transactions for account: {}", accountNumber);
        
        List<TransactionResponse> transactions = transactionService.getAccountTransactions(accountNumber);
        
        return ResponseEntity.ok(ApiResponse.success("Account transactions retrieved successfully", transactions));
    }

    @GetMapping("/account/{accountNumber}/paged")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAccountTransactionsPaged(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("Retrieving paged transactions for account: {} (page: {}, size: {})", 
                accountNumber, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionResponse> transactions = transactionService.getAccountTransactions(accountNumber, pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Account transactions retrieved successfully", transactions));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getCustomerTransactions(@PathVariable Long customerId) {
        logger.info("Retrieving transactions for customer: {}", customerId);
        
        List<TransactionResponse> transactions = transactionService.getCustomerTransactions(customerId);
        
        return ResponseEntity.ok(ApiResponse.success("Customer transactions retrieved successfully", transactions));
    }
}