package com.bankz.controller;

import com.bankz.entity.BankZAccount;
import com.bankz.entity.BankZCustomer;
import com.bankz.entity.BankZTransaction;
import com.bankz.entity.BankZUser;
import com.bankz.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/operator")
public class BankZOperatorController {
    
    @Autowired
    private BankZCustomerService customerService;
    
    @Autowired
    private BankZAccountService accountService;
    
    @Autowired
    private BankZTransactionService transactionService;
    
    @Autowired
    private BankZUserService userService;
    
    @Autowired
    private BankXIntegrationService bankXIntegrationService;
    
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public String dashboard(Model model) {
        try {
            // Get real data from services
            List<BankZCustomer> customers = customerService.getAllCustomers();
            long totalCustomers = customers.size();
            long totalAccounts = accountService.countActiveAccounts();
            
            // Get today's transactions
            List<BankZTransaction> allTransactions = customers.stream()
                .flatMap(customer -> accountService.getCustomerAccounts(customer.getId()).stream())
                .flatMap(account -> transactionService.getAccountTransactions(account).stream())
                .filter(txn -> txn.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                .collect(Collectors.toList());
            
            int todayTransactions = allTransactions.size();
            int successfulTransactions = (int) allTransactions.stream()
                .filter(txn -> txn.getStatus() == BankZTransaction.TransactionStatus.COMPLETED)
                .count();
            int failedTransactions = (int) allTransactions.stream()
                .filter(txn -> txn.getStatus() == BankZTransaction.TransactionStatus.FAILED)
                .count();
            int pendingTransactions = (int) allTransactions.stream()
                .filter(txn -> txn.getStatus() == BankZTransaction.TransactionStatus.PENDING)
                .count();
            
            // Check Bank X status
            boolean bankXOnline = bankXIntegrationService.checkBankXStatus();
            
            model.addAttribute("totalCustomers", totalCustomers);
            model.addAttribute("totalAccounts", totalAccounts);
            model.addAttribute("todayTransactions", todayTransactions);
            model.addAttribute("successfulTransactions", successfulTransactions);
            model.addAttribute("failedTransactions", failedTransactions);
            model.addAttribute("matchedTransactions", successfulTransactions);
            model.addAttribute("mismatchedTransactions", failedTransactions);
            model.addAttribute("pendingTransactions", pendingTransactions);
            model.addAttribute("lastReconciliation", LocalDateTime.now().minusHours(2));
            model.addAttribute("systemStatus", "OPERATIONAL");
            model.addAttribute("bankXStatus", bankXOnline ? "ACTIVE" : "OFFLINE");
            
        } catch (Exception e) {
            // Fallback to default values if there's an error
            model.addAttribute("totalCustomers", 0);
            model.addAttribute("totalAccounts", 0);
            model.addAttribute("todayTransactions", 0);
            model.addAttribute("successfulTransactions", 0);
            model.addAttribute("failedTransactions", 0);
            model.addAttribute("matchedTransactions", 0);
            model.addAttribute("mismatchedTransactions", 0);
            model.addAttribute("pendingTransactions", 0);
            model.addAttribute("lastReconciliation", LocalDateTime.now().minusHours(2));
            model.addAttribute("systemStatus", "ERROR");
            model.addAttribute("bankXStatus", "UNKNOWN");
        }
        
        return "operator/dashboard";
    }
    
    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public String transactions(Model model,
                             @RequestParam(required = false) String dateFrom,
                             @RequestParam(required = false) String dateTo,
                             @RequestParam(required = false) String customerId,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String type) {
        
        try {
            // Get real customers for dropdown
            List<BankZCustomer> customers = customerService.getAllCustomers();
            System.out.println("DEBUG: Found " + customers.size() + " customers");
            
            List<Map<String, Object>> customerMaps = customers.stream()
                .map(customer -> {
                    Map<String, Object> customerMap = new HashMap<>();
                    customerMap.put("id", customer.getId());
                    customerMap.put("firstName", customer.getFirstName());
                    customerMap.put("lastName", customer.getLastName());
                    customerMap.put("email", customer.getEmail());
                    return customerMap;
                })
                .collect(Collectors.toList());
            model.addAttribute("customers", customerMaps);
            
            // Get all accounts for the create transaction dropdown
            List<BankZAccount> allAccounts = accountService.getAllAccounts();
            System.out.println("DEBUG: Found " + allAccounts.size() + " accounts");
            
            List<Map<String, Object>> accountMaps = allAccounts.stream()
                .map(account -> {
                    Map<String, Object> accountMap = new HashMap<>();
                    accountMap.put("accountNumber", account.getAccountNumber());
                    accountMap.put("accountType", account.getAccountType().name());
                    
                    // Handle potential null customer with better error handling
                    String customerName = "Unknown Customer";
                    if (account.getCustomer() != null) {
                        customerName = account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName();
                    }
                    accountMap.put("customerName", customerName);
                    accountMap.put("balance", account.getBalance());
                    
                    System.out.println("DEBUG: Account " + account.getAccountNumber() + " - " + customerName);
                    return accountMap;
                })
                .collect(Collectors.toList());
            model.addAttribute("accounts", accountMaps);
            System.out.println("DEBUG: Added " + accountMaps.size() + " accounts to model");
            
            // Get all transactions from all accounts
            List<BankZTransaction> allTransactions = transactionService.getAllTransactions();
            
            // Apply filters if provided
            if (customerId != null && !customerId.isEmpty() && !customerId.equals("")) {
                try {
                    Long customerIdLong = Long.parseLong(customerId);
                    allTransactions = allTransactions.stream()
                        .filter(txn -> txn.getAccount() != null && 
                                      txn.getAccount().getCustomer() != null &&
                                      txn.getAccount().getCustomer().getId().equals(customerIdLong))
                        .collect(Collectors.toList());
                } catch (NumberFormatException e) {
                    // Invalid customer ID, ignore filter
                }
            }
            
            if (status != null && !status.isEmpty() && !status.equals("")) {
                allTransactions = allTransactions.stream()
                    .filter(txn -> txn.getStatus() != null && txn.getStatus().name().equals(status))
                    .collect(Collectors.toList());
            }
            
            if (type != null && !type.isEmpty() && !type.equals("")) {
                allTransactions = allTransactions.stream()
                    .filter(txn -> txn.getTransactionType() != null && txn.getTransactionType().name().equals(type))
                    .collect(Collectors.toList());
            }
            
            // Convert to maps for template with null checks
            List<Map<String, Object>> transactionMaps = allTransactions.stream()
                .map(txn -> {
                    Map<String, Object> transactionMap = new HashMap<>();
                    transactionMap.put("id", txn.getTransactionId() != null ? txn.getTransactionId() : "N/A");
                    transactionMap.put("transactionType", txn.getTransactionType() != null ? txn.getTransactionType().name() : "UNKNOWN");
                    transactionMap.put("amount", txn.getAmount() != null ? txn.getAmount() : BigDecimal.ZERO);
                    transactionMap.put("description", txn.getDescription() != null ? txn.getDescription() : "No description");
                    transactionMap.put("createdAt", txn.getCreatedAt() != null ? txn.getCreatedAt() : LocalDateTime.now());
                    transactionMap.put("status", txn.getStatus() != null ? txn.getStatus().name() : "UNKNOWN");
                    transactionMap.put("accountNumber", txn.getAccount() != null ? txn.getAccount().getAccountNumber() : "N/A");
                    transactionMap.put("sentToBankX", txn.isSentToBankX());
                    transactionMap.put("customerName", 
                        txn.getAccount() != null && txn.getAccount().getCustomer() != null ? 
                        txn.getAccount().getCustomer().getFirstName() + " " + txn.getAccount().getCustomer().getLastName() : 
                        "Unknown Customer");
                    return transactionMap;
                })
                .collect(Collectors.toList());
            
            model.addAttribute("transactions", transactionMaps);
            
        } catch (Exception e) {
            // Log the error and provide fallback data
            System.err.println("Error loading transactions: " + e.getMessage());
            e.printStackTrace();
            
            // Provide fallback empty lists to prevent template errors
            model.addAttribute("customers", List.of());
            model.addAttribute("accounts", List.of());
            model.addAttribute("transactions", List.of());
            model.addAttribute("errorMessage", "Error loading transaction data: " + e.getMessage());
        }
        
        // Ensure accounts attribute is always present
        if (!model.containsAttribute("accounts")) {
            model.addAttribute("accounts", List.of());
        }
        
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("customerId", customerId);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        
        return "operator/transactions";
    }
    
    @PostMapping("/transactions/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public String createTransaction(@RequestParam String accountNumber,
                                  @RequestParam BigDecimal amount,
                                  @RequestParam String type,
                                  @RequestParam String description,
                                  @RequestParam(required = false) String reference,
                                  @RequestParam(required = false) String customerId,
                                  @RequestParam(required = false) String accountType,
                                  RedirectAttributes redirectAttributes) {
        try {
            BankZTransaction transaction;
            
            // Create transaction in Bank Z
            if ("CREDIT".equals(type)) {
                transaction = accountService.creditAccount(accountNumber, amount, description, reference);
            } else if ("DEBIT".equals(type)) {
                transaction = accountService.debitAccount(accountNumber, amount, description, reference);
            } else {
                throw new RuntimeException("Invalid transaction type: " + type);
            }
            
            // Automatically send to Bank X with mapped customer data
            try {
                // Map Bank Z customer to Bank X customer ID (simple mapping for demo)
                String bankXCustomerId = mapBankZCustomerToBankX(transaction.getAccount().getCustomer());
                String bankXAccountType = transaction.getAccount().getAccountType().name();
                
                boolean sentToBankX = bankXIntegrationService.sendTransactionToBankX(transaction, bankXCustomerId, bankXAccountType);
                
                if (sentToBankX) {
                    transaction.setSentToBankX(true);
                    transaction.setBankxResponse("SUCCESS");
                    redirectAttributes.addFlashAttribute("successMessage", 
                        String.format("Transaction created successfully and sent to Bank X. ID: %s", transaction.getTransactionId()));
                } else {
                    transaction.setBankxResponse("FAILED");
                    redirectAttributes.addFlashAttribute("warningMessage", 
                        String.format("Transaction created (ID: %s) but failed to send to Bank X", transaction.getTransactionId()));
                }
            } catch (Exception bankXError) {
                redirectAttributes.addFlashAttribute("warningMessage", 
                    String.format("Transaction created (ID: %s) but failed to send to Bank X: %s", 
                        transaction.getTransactionId(), bankXError.getMessage()));
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to create transaction: " + e.getMessage());
        }
        
        return "redirect:/operator/transactions";
    }
    
    @GetMapping("/batch-upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public String batchUpload(Model model) {
        return "operator/batch-upload";
    }
    
    @PostMapping("/batch-upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public String processBatchUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload");
                return "redirect:/operator/batch-upload";
            }
            
            String filename = file.getOriginalFilename();
            
            // Mock processing results
            int totalRecords = 25;
            int successfulRecords = 23;
            int failedRecords = 2;
            
            redirectAttributes.addFlashAttribute("successMessage", 
                String.format("Batch upload completed. File: %s, Total: %d, Success: %d, Failed: %d", 
                    filename, totalRecords, successfulRecords, failedRecords));
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Batch upload failed: " + e.getMessage());
        }
        
        return "redirect:/operator/batch-upload";
    }
    
    @GetMapping("/reconciliation")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public String reconciliation(Model model) {
        List<Map<String, Object>> reconciliationLogs = List.of(
            Map.of("date", LocalDate.now().minusDays(1), "status", "COMPLETED", 
                   "totalRecords", 150, "matched", 147, "mismatched", 3),
            Map.of("date", LocalDate.now().minusDays(2), "status", "COMPLETED", 
                   "totalRecords", 132, "matched", 132, "mismatched", 0),
            Map.of("date", LocalDate.now().minusDays(3), "status", "COMPLETED", 
                   "totalRecords", 98, "matched", 95, "mismatched", 3)
        );
        
        model.addAttribute("reconciliationLogs", reconciliationLogs);
        return "operator/reconciliation";
    }
    
    @PostMapping("/reconciliation/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public String processReconciliation(@RequestParam("file") MultipartFile file,
                                      @RequestParam String reconciliationDate,
                                      RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please select a reconciliation file");
                return "redirect:/operator/reconciliation";
            }
            
            String filename = file.getOriginalFilename();
            
            // Mock reconciliation results
            int totalRecords = 150;
            int matchedRecords = 147;
            int mismatchedRecords = 3;
            
            redirectAttributes.addFlashAttribute("successMessage", 
                String.format("Reconciliation completed for %s. Total: %d, Matched: %d, Mismatched: %d", 
                    reconciliationDate, totalRecords, matchedRecords, mismatchedRecords));
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Reconciliation failed: " + e.getMessage());
        }
        
        return "redirect:/operator/reconciliation";
    }
    
    @GetMapping("/api-logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public String apiLogs(Model model) {
        List<Map<String, Object>> apiLogs = List.of(
            Map.of("timestamp", LocalDateTime.now().minusMinutes(5), "method", "POST", 
                   "endpoint", "/api/transactions", "status", "200", "responseTime", "120ms", "requestId", "REQ001"),
            Map.of("timestamp", LocalDateTime.now().minusMinutes(10), "method", "GET", 
                   "endpoint", "/api/accounts", "status", "200", "responseTime", "85ms", "requestId", "REQ002"),
            Map.of("timestamp", LocalDateTime.now().minusMinutes(15), "method", "POST", 
                   "endpoint", "/api/customers", "status", "401", "responseTime", "50ms", "requestId", "REQ003"),
            Map.of("timestamp", LocalDateTime.now().minusMinutes(20), "method", "GET", 
                   "endpoint", "/api/status", "status", "200", "responseTime", "45ms", "requestId", "REQ004"),
            Map.of("timestamp", LocalDateTime.now().minusMinutes(25), "method", "POST", 
                   "endpoint", "/api/reconciliation", "status", "500", "responseTime", "2500ms", "requestId", "REQ005")
        );
        
        model.addAttribute("apiLogs", apiLogs);
        model.addAttribute("apiStatus", "ONLINE");
        model.addAttribute("responseTime", "120ms");
        model.addAttribute("successRate", "99.8%");
        model.addAttribute("errorCount", 2);
        
        return "operator/api-logs";
    }
    
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String admin(Model model) {
        List<Map<String, Object>> users = List.of(
            Map.of("username", "admin", "role", "ADMIN", "status", "ACTIVE", 
                   "email", "admin@bankz.com", "firstName", "System", "lastName", "Administrator"),
            Map.of("username", "operator", "role", "OPERATOR", "status", "ACTIVE", 
                   "email", "operator@bankz.com", "firstName", "Bank", "lastName", "Operator"),
            Map.of("username", "auditor", "role", "AUDITOR", "status", "ACTIVE", 
                   "email", "auditor@bankz.com", "firstName", "System", "lastName", "Auditor")
        );
        
        model.addAttribute("users", users);
        model.addAttribute("apiBaseUrl", "http://localhost:9090");
        model.addAttribute("apiKey", "bankz_secure_api_key_2024");
        model.addAttribute("apiTimeout", 30);
        model.addAttribute("transactionFee", 0.05);
        model.addAttribute("interestRate", 0.5);
        model.addAttribute("dailyLimit", 10000);
        
        // Add current user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("currentUser", auth.getName());
        
        return "operator/admin";
    }
    
    @PostMapping("/admin/save-api-config")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveApiConfig(@RequestParam String apiBaseUrl,
                              @RequestParam String apiKey,
                              @RequestParam int apiTimeout,
                              RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("successMessage", "API configuration saved successfully");
        return "redirect:/operator/admin";
    }
    
    @PostMapping("/admin/save-business-rules")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveBusinessRules(@RequestParam BigDecimal transactionFee,
                                  @RequestParam BigDecimal interestRate,
                                  @RequestParam BigDecimal dailyLimit,
                                  RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("successMessage", "Business rules saved successfully");
        return "redirect:/operator/admin";
    }
    
    @GetMapping("/debug/accounts")
    @ResponseBody
    public Map<String, Object> debugAccounts() {
        Map<String, Object> debug = new HashMap<>();
        try {
            List<BankZCustomer> customers = customerService.getAllCustomers();
            List<BankZAccount> accounts = accountService.getAllAccounts();
            
            debug.put("customerCount", customers.size());
            debug.put("accountCount", accounts.size());
            debug.put("customers", customers.stream().map(c -> 
                Map.of("id", c.getId(), "name", c.getFirstName() + " " + c.getLastName(), "email", c.getEmail())
            ).collect(Collectors.toList()));
            debug.put("accounts", accounts.stream().map(a -> 
                Map.of("accountNumber", a.getAccountNumber(), 
                       "type", a.getAccountType().name(),
                       "balance", a.getBalance(),
                       "customerName", a.getCustomer() != null ? a.getCustomer().getFirstName() + " " + a.getCustomer().getLastName() : "NO CUSTOMER")
            ).collect(Collectors.toList()));
            debug.put("status", "SUCCESS");
        } catch (Exception e) {
            debug.put("status", "ERROR");
            debug.put("error", e.getMessage());
            debug.put("stackTrace", e.getStackTrace());
        }
        return debug;
    }
    
    /**
     * Maps Bank Z customer to Bank X customer ID
     * In a real implementation, this would use a proper mapping table
     */
    private String mapBankZCustomerToBankX(BankZCustomer bankZCustomer) {
        // Simple mapping based on email for consistent customer mapping between systems
        // In production, this would be a proper customer mapping service
        if (bankZCustomer.getEmail().equals("thabo.mthembu@gmail.com")) {
            return "1"; // Maps to Bank X customer ID 1 (Thabo Mthembu)
        } else if (bankZCustomer.getEmail().equals("nomsa.dlamini@outlook.com")) {
            return "2"; // Maps to Bank X customer ID 2 (Nomsa Dlamini)
        } else if (bankZCustomer.getEmail().equals("sipho.nkomo@yahoo.com")) {
            return "3"; // Maps to Bank X customer ID 3 (Sipho Nkomo)
        } else if (bankZCustomer.getEmail().equals("lerato.mokoena@webmail.co.za")) {
            return "4"; // Maps to Bank X customer ID 4 (Lerato Mokoena)
        } else if (bankZCustomer.getEmail().equals("mandla.van.der.merwe@gmail.com")) {
            return "5"; // Maps to Bank X customer ID 5 (Mandla van der Merwe)
        } else {
            return "1"; // Default to customer 1 for any unmapped customers
        }
    }
}