package com.bankx.controller;

import com.bankx.entity.Account;
import com.bankx.entity.Customer;
import com.bankx.entity.Transaction;
import com.bankx.entity.User;
import com.bankx.service.AccountService;
import com.bankx.service.CustomerService;
import com.bankx.service.TransactionService;
import com.bankx.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/customer")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user != null && user.getCustomer() != null) {
            Customer customer = user.getCustomer();
            List<Account> accounts = accountService.getAccountsByCustomerId(customer.getId());
            List<Transaction> recentTransactions = transactionService.getTransactionsByCustomerId(customer.getId(), 5);
            
            model.addAttribute("customer", customer);
            model.addAttribute("accounts", accounts);
            model.addAttribute("recentTransactions", recentTransactions);
        }
        return "customer/dashboard";
    }

    @GetMapping("/accounts")
    public String accounts(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user != null && user.getCustomer() != null) {
            List<Account> accounts = accountService.getAccountsByCustomerId(user.getCustomer().getId());
            model.addAttribute("accounts", accounts);
        }
        return "customer/accounts";
    }

    @GetMapping("/transfer")
    public String transfer(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user != null && user.getCustomer() != null) {
            List<Account> accounts = accountService.getAccountsByCustomerId(user.getCustomer().getId());
            model.addAttribute("accounts", accounts);
        }
        return "customer/transfer";
    }

    @PostMapping("/transfer")
    public String processTransfer(@RequestParam String fromAccount,
                                 @RequestParam String toAccount,
                                 @RequestParam BigDecimal amount,
                                 @RequestParam String description,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            accountService.transferBetweenAccounts(fromAccount, toAccount, amount, description);
            redirectAttributes.addFlashAttribute("success", "Transfer completed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Transfer failed: " + e.getMessage());
        }
        return "redirect:/customer/transfer";
    }

    @GetMapping("/transactions")
    public String transactions(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName()).orElse(null);
        if (user != null && user.getCustomer() != null) {
            List<Transaction> transactions = transactionService.getTransactionsByCustomerId(user.getCustomer().getId(), 50);
            model.addAttribute("transactions", transactions);
        }
        return "customer/transactions";
    }
}