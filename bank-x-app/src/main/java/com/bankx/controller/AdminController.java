package com.bankx.controller;

import com.bankx.dto.ApiResponse;
import com.bankx.dto.CustomerResponse;
import com.bankx.entity.Account;
import com.bankx.entity.BankZTransaction;
import com.bankx.entity.Customer;
import com.bankx.entity.Transaction;
import com.bankx.service.AccountService;
import com.bankx.service.BankZIntegrationService;
import com.bankx.service.CustomerService;
import com.bankx.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Controller for Bank Z Interface Management
 * Provides administrative functions for monitoring and managing Bank Z transactions
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BankZIntegrationService bankZIntegrationService;

    /**
     * Get system dashboard overview
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        logger.info("Admin dashboard requested");

        Map<String, Object> dashboard = new HashMap<>();
        
        // Get basic statistics
        List<CustomerResponse> allCustomers = customerService.getAllCustomers();
        dashboard.put("totalCustomers", allCustomers.size());
        
        // Calculate total accounts and balances
        int totalAccounts = 0;
        BigDecimal totalBalance = BigDecimal.ZERO;
        
        for (CustomerResponse customerResponse : allCustomers) {
            Customer customer = customerService.getCustomerById(customerResponse.getId());
            List<Account> customerAccounts = accountService.getAccountsByCustomerId(customer.getId());
            totalAccounts += customerAccounts.size();
            
            for (Account account : customerAccounts) {
                totalBalance = totalBalance.add(account.getBalance());
            }
        }
        
        dashboard.put("totalAccounts", totalAccounts);
        dashboard.put("totalSystemBalance", totalBalance);
        dashboard.put("lastUpdated", LocalDateTime.now());
        
        return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved successfully", dashboard));
    }

    /**
     * Get all customers with pagination
     */
    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<Page<Customer>>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("Admin customers list requested - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Customer> customers = customerService.getAllCustomersPaginated(pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", customers));
    }

    /**
     * Get customer details with accounts
     */
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomerDetails(@PathVariable Long customerId) {
        logger.info("Admin customer details requested for customer: {}", customerId);
        
        Customer customer = customerService.getCustomerById(customerId);
        List<Account> accounts = accountService.getAccountsByCustomerId(customerId);
        
        Map<String, Object> customerDetails = new HashMap<>();
        customerDetails.put("customer", customer);
        customerDetails.put("accounts", accounts);
        
        return ResponseEntity.ok(ApiResponse.success("Customer details retrieved successfully", customerDetails));
    }

    /**
     * Get all transactions with pagination
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<Transaction>>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        logger.info("Admin transactions list requested - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionService.getAllTransactionsPaginated(pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved successfully", transactions));
    }

    /**
     * Get Bank Z transactions with pagination
     */
    @GetMapping("/bankz-transactions")
    public ResponseEntity<ApiResponse<Page<BankZTransaction>>> getBankZTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        logger.info("Admin Bank Z transactions list requested - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BankZTransaction> bankZTransactions = bankZIntegrationService.getAllBankZTransactionsPaginated(pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Bank Z transactions retrieved successfully", bankZTransactions));
    }

    /**
     * Get Bank Z transaction statistics
     */
    @GetMapping("/bankz-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBankZStats() {
        logger.info("Admin Bank Z statistics requested");
        
        Map<String, Object> stats = bankZIntegrationService.getBankZTransactionStats();
        
        return ResponseEntity.ok(ApiResponse.success("Bank Z statistics retrieved successfully", stats));
    }

    /**
     * Get failed Bank Z transactions for manual reconciliation
     */
    @GetMapping("/bankz-failed")
    public ResponseEntity<ApiResponse<List<BankZTransaction>>> getFailedBankZTransactions() {
        logger.info("Admin failed Bank Z transactions requested");
        
        List<BankZTransaction> failedTransactions = bankZIntegrationService.getFailedBankZTransactions();
        
        return ResponseEntity.ok(ApiResponse.success("Failed Bank Z transactions retrieved successfully", failedTransactions));
    }

    /**
     * Manual reconciliation endpoint
     */
    @PostMapping("/bankz-reconcile/{transactionId}")
    public ResponseEntity<ApiResponse<String>> manualReconciliation(@PathVariable Long transactionId) {
        logger.info("Manual reconciliation requested for Bank Z transaction: {}", transactionId);
        
        bankZIntegrationService.performManualReconciliation(transactionId);
        
        return ResponseEntity.ok(ApiResponse.success("Manual reconciliation completed successfully"));
    }

    /**
     * Get system health and performance metrics
     */
    @GetMapping("/system-health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        logger.info("Admin system health requested");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "HEALTHY");
        health.put("timestamp", LocalDateTime.now());
        health.put("uptime", getSystemUptime());
        health.put("memoryUsage", getMemoryUsage());
        
        return ResponseEntity.ok(ApiResponse.success("System health retrieved successfully", health));
    }

    private String getSystemUptime() {
        long uptimeMillis = System.currentTimeMillis() - getStartTime();
        long uptimeSeconds = uptimeMillis / 1000;
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;
        
        return String.format("%d hours, %d minutes, %d seconds", hours, minutes, seconds);
    }

    private long getStartTime() {
        // This would typically be stored when the application starts
        // For now, we'll use a placeholder
        return System.currentTimeMillis() - (2 * 60 * 60 * 1000); // 2 hours ago
    }

    private Map<String, Object> getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        memory.put("maxMemory", formatBytes(maxMemory));
        memory.put("totalMemory", formatBytes(totalMemory));
        memory.put("usedMemory", formatBytes(usedMemory));
        memory.put("freeMemory", formatBytes(freeMemory));
        memory.put("usagePercentage", Math.round((double) usedMemory / totalMemory * 100));
        
        return memory;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}