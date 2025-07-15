package com.bankx.controller;

import com.bankx.dto.AccountResponse;
import com.bankx.dto.ApiResponse;
import com.bankx.dto.TransferRequest;
import com.bankx.service.AccountService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private AccountService accountService;

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable String accountNumber) {
        logger.info("Retrieving account: {}", accountNumber);
        
        AccountResponse account = accountService.getAccount(accountNumber);
        
        return ResponseEntity.ok(ApiResponse.success("Account retrieved successfully", account));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getAccountBalance(@PathVariable String accountNumber) {
        logger.info("Retrieving balance for account: {}", accountNumber);
        
        BigDecimal balance = accountService.getAccountBalance(accountNumber);
        
        return ResponseEntity.ok(ApiResponse.success("Account balance retrieved successfully", balance));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getCustomerAccounts(@PathVariable Long customerId) {
        logger.info("Retrieving accounts for customer: {}", customerId);
        
        List<AccountResponse> accounts = accountService.getCustomerAccounts(customerId);
        
        return ResponseEntity.ok(ApiResponse.success("Customer accounts retrieved successfully", accounts));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<String>> transferBetweenAccounts(@Valid @RequestBody TransferRequest request) {
        logger.info("Processing transfer from {} to {} amount: {}", 
                request.getFromAccountNumber(), request.getToAccountNumber(), request.getAmount());
        
        accountService.transferBetweenAccounts(request);
        
        return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully"));
    }
}