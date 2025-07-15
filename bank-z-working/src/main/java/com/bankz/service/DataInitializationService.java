package com.bankz.service;

import com.bankz.entity.BankZAccount;
import com.bankz.entity.BankZCustomer;
import com.bankz.entity.BankZUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DataInitializationService implements CommandLineRunner {
    
    @Autowired
    private BankZUserService userService;
    
    @Autowired
    private BankZCustomerService customerService;
    
    @Autowired
    private BankZAccountService accountService;
    
    @Override
    public void run(String... args) throws Exception {
        initializeDefaultUsers();
        initializeSampleCustomers();
        printStartupInfo();
    }
    
    private void initializeDefaultUsers() {
        // Create Admin user
        if (!userService.existsByUsername("admin")) {
            userService.createUser(
                "admin",
                "admin@bankz.com",
                "admin123",
                "System",
                "Administrator",
                BankZUser.UserRole.ADMIN
            );
            System.out.println("✅ BankZ Admin user created: admin / admin123");
        }
        
        // Create Operator user
        if (!userService.existsByUsername("operator")) {
            userService.createUser(
                "operator",
                "operator@bankz.com",
                "operator123",
                "Bank",
                "Operator",
                BankZUser.UserRole.OPERATOR
            );
            System.out.println("✅ BankZ Operator user created: operator / operator123");
        }
        
        // Create Auditor user
        if (!userService.existsByUsername("auditor")) {
            userService.createUser(
                "auditor",
                "auditor@bankz.com",
                "auditor123",
                "System",
                "Auditor",
                BankZUser.UserRole.AUDITOR
            );
            System.out.println("✅ BankZ Auditor user created: auditor / auditor123");
        }
    }
    
    private void initializeSampleCustomers() {
        // Create customers with authentic South African names
        createCustomerSafely("thabo.mthembu@gmail.com", "Thabo", "Mthembu", 
            "+27821234567", "8001015009087", "123 Mandela Street, Soweto, Johannesburg",
            "BZ-ACC-001", "BZ-ACC-002");
            
        createCustomerSafely("nomsa.dlamini@outlook.com", "Nomsa", "Dlamini", 
            "+27834567890", "8505125009088", "456 Vilakazi Street, Orlando West, Soweto",
            "BZ-ACC-003", "BZ-ACC-004");
            
        createCustomerSafely("sipho.nkomo@yahoo.com", "Sipho", "Nkomo", 
            "+27798765432", "7012255009089", "789 Beyers Naude Drive, Randburg",
            "BZ-ACC-005", "BZ-ACC-006");
            
        createCustomerSafely("lerato.mokoena@webmail.co.za", "Lerato", "Mokoena", 
            "+27765432109", "9203155009090", "321 Jan Smuts Avenue, Rosebank",
            "BZ-ACC-007", "BZ-ACC-008");
            
        createCustomerSafely("mandla.van.der.merwe@gmail.com", "Mandla", "van der Merwe", 
            "+27712345678", "8109155009091", "654 Long Street, Cape Town City Centre",
            "BZ-ACC-009", "BZ-ACC-010");
    }
    
    private void createCustomerSafely(String email, String firstName, String lastName, 
                                    String phone, String idNumber, String address,
                                    String currentAccountNumber, String savingsAccountNumber) {
        try {
            if (!customerService.existsByEmail(email)) {
                BankZCustomer customer = customerService.createCustomer(
                    firstName, lastName, email, phone, idNumber, address
                );
                
                // Create accounts for customer
                accountService.createAccount(customer, currentAccountNumber, BankZAccount.AccountType.CURRENT);
                accountService.createAccount(customer, savingsAccountNumber, BankZAccount.AccountType.SAVINGS);
                
                System.out.println("✅ Customer created: " + firstName + " " + lastName);
            } else {
                System.out.println("✅ Customer already exists: " + firstName + " " + lastName);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error creating customer " + firstName + " " + lastName + ": " + e.getMessage());
            // Continue with other customers instead of failing completely
        }
    }
    
    private void printStartupInfo() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🏛️  BANK Z APPLICATION STARTED SUCCESSFULLY");
        System.out.println("=".repeat(60));
        System.out.println("📍 Access BankZ at: http://localhost:9091/login");
        System.out.println("👤 Admin Login: admin / admin123");
        System.out.println("👤 Operator Login: operator / operator123");
        System.out.println("👤 Auditor Login: auditor / auditor123");
        System.out.println("=".repeat(60));
        System.out.println("🔗 Bank X Integration: http://localhost:9090");
        System.out.println("📊 Sample customers and accounts created");
        System.out.println("=".repeat(60) + "\n");
    }
}