package com.bankx.controller;

import com.bankx.entity.User;
import com.bankx.entity.Customer;
import com.bankx.entity.Account;
import com.bankx.entity.Transaction;
import com.bankx.dto.CustomerResponse;
import com.bankx.service.UserService;
import com.bankx.service.CustomerService;
import com.bankx.service.AccountService;
import com.bankx.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@Controller
public class WebController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private TransactionService transactionService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "logout", required = false) String logout,
                       Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                              BindingResult result,
                              @RequestParam("confirmPassword") String confirmPassword,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "register";
        }

        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "register";
        }

        if (userService.existsByUsername(user.getUsername())) {
            model.addAttribute("error", "Username already exists");
            return "register";
        }

        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email already exists");
            return "register";
        }

        // Validate SA ID number if provided
        if (user.getSaIdNumber() != null && !user.getSaIdNumber().trim().isEmpty()) {
            String saId = user.getSaIdNumber().trim();
            if (!saId.matches("\\d{13}")) {
                model.addAttribute("error", "South African ID number must be exactly 13 digits");
                return "register";
            }
            
            // Check if SA ID already exists
            if (userService.existsBySaIdNumber(saId)) {
                model.addAttribute("error", "South African ID number already registered");
                return "register";
            }
            user.setSaIdNumber(saId);
        } else {
            user.setSaIdNumber(null);
        }

        try {
            userService.createCustomerUser(
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getSaIdNumber()
            );
            
            redirectAttributes.addFlashAttribute("message", 
                "Registration successful! You can now login with your credentials.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {
        try {
            // Get basic statistics
            List<CustomerResponse> allCustomers = customerService.getAllCustomers();
            model.addAttribute("totalCustomers", allCustomers.size());
            
            // Calculate total accounts and balances
            int totalAccounts = 0;
            BigDecimal totalDeposits = BigDecimal.ZERO;
            
            for (CustomerResponse customerResponse : allCustomers) {
                Customer customer = customerService.getCustomerById(customerResponse.getId());
                List<Account> customerAccounts = accountService.getAccountsByCustomerId(customer.getId());
                totalAccounts += customerAccounts.size();
                
                for (Account account : customerAccounts) {
                    totalDeposits = totalDeposits.add(account.getBalance());
                }
            }
            
            model.addAttribute("totalAccounts", totalAccounts);
            model.addAttribute("totalDeposits", totalDeposits);
            
            // Get recent transactions (last 10)
            List<Transaction> recentTransactions = transactionService.getAllTransactionsPaginated(PageRequest.of(0, 10)).getContent();
            model.addAttribute("recentTransactions", recentTransactions);
            
            // Add some default values for template variables
            model.addAttribute("monthlyCustomerChange", 0.0);
            model.addAttribute("monthlyAccountChange", 0.0);
            model.addAttribute("transactionCountChange", 0.0);
            model.addAttribute("weeklyVolumeChange", 0.0);
            model.addAttribute("dailyTransactionCount", recentTransactions.size());
            model.addAttribute("dailyTransactionVolume", totalDeposits);
            model.addAttribute("dailyNewCustomers", 0);
            model.addAttribute("customerChange", 0.0);
            model.addAttribute("volumeChange", 0.0);
            model.addAttribute("securityEvents", 0);
            
        } catch (Exception e) {
            // If there's an error, provide default values
            model.addAttribute("totalCustomers", 0);
            model.addAttribute("totalAccounts", 0);
            model.addAttribute("totalDeposits", BigDecimal.ZERO);
            model.addAttribute("recentTransactions", List.of());
            model.addAttribute("monthlyCustomerChange", 0.0);
            model.addAttribute("monthlyAccountChange", 0.0);
            model.addAttribute("transactionCountChange", 0.0);
            model.addAttribute("weeklyVolumeChange", 0.0);
            model.addAttribute("dailyTransactionCount", 0);
            model.addAttribute("dailyTransactionVolume", BigDecimal.ZERO);
            model.addAttribute("dailyNewCustomers", 0);
            model.addAttribute("customerChange", 0.0);
            model.addAttribute("volumeChange", 0.0);
            model.addAttribute("securityEvents", 0);
        }
        
        return "admin/dashboard";
    }

    @GetMapping("/admin/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminCustomers(Model model) {
        try {
            List<CustomerResponse> customers = customerService.getAllCustomers();
            System.out.println("DEBUG: Found " + customers.size() + " customers for admin page");
            model.addAttribute("customers", customers);
            model.addAttribute("totalCustomers", customers.size());
        } catch (Exception e) {
            System.err.println("ERROR loading customers: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("customers", List.of());
            model.addAttribute("totalCustomers", 0);
            model.addAttribute("errorMessage", "Error loading customers: " + e.getMessage());
        }
        return "admin/customers";
    }

    @GetMapping("/admin/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminTransactions(Model model) {
        try {
            List<Transaction> transactions = transactionService.getAllTransactionsPaginated(PageRequest.of(0, 100)).getContent();
            System.out.println("DEBUG: Found " + transactions.size() + " transactions for admin page");
            model.addAttribute("transactions", transactions);
            model.addAttribute("totalTransactions", transactions.size());
        } catch (Exception e) {
            System.err.println("ERROR loading transactions: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("transactions", List.of());
            model.addAttribute("totalTransactions", 0);
            model.addAttribute("errorMessage", "Error loading transactions: " + e.getMessage());
        }
        return "admin/transactions";
    }

    @GetMapping("/admin/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccounts(Model model) {
        try {
            List<CustomerResponse> customers = customerService.getAllCustomers();
            List<Account> allAccounts = new java.util.ArrayList<>();
            
            System.out.println("DEBUG: Found " + customers.size() + " customers for accounts page");
            
            // Get all accounts from all customers
            for (CustomerResponse customerResponse : customers) {
                Customer customer = customerService.getCustomerById(customerResponse.getId());
                List<Account> customerAccounts = accountService.getAccountsByCustomerId(customer.getId());
                System.out.println("DEBUG: Customer " + customer.getId() + " has " + customerAccounts.size() + " accounts");
                allAccounts.addAll(customerAccounts);
            }
            
            System.out.println("DEBUG: Total accounts found: " + allAccounts.size());
            model.addAttribute("accounts", allAccounts);
            model.addAttribute("totalAccounts", allAccounts.size());
        } catch (Exception e) {
            System.err.println("ERROR loading accounts: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("accounts", List.of());
            model.addAttribute("totalAccounts", 0);
            model.addAttribute("errorMessage", "Error loading accounts: " + e.getMessage());
        }
        return "admin/accounts";
    }

    @GetMapping("/admin/customers/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminCustomerDetails(@PathVariable Long customerId, Model model) {
        try {
            Customer customer = customerService.getCustomerById(customerId);
            List<Account> accounts = accountService.getAccountsByCustomerId(customerId);
            
            model.addAttribute("customer", customer);
            model.addAttribute("accounts", accounts);
            model.addAttribute("customerId", customerId);
        } catch (Exception e) {
            System.err.println("ERROR loading customer details: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Error loading customer details: " + e.getMessage());
            return "redirect:/admin/customers";
        }
        return "admin/customer-details";
    }
}