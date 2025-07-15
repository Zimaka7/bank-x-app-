package com.bankx.service;

import com.bankx.entity.User;
import com.bankx.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        try {
            initializeAdminUser();
            initializeSampleCustomers();
            printStartupInfo();
        } catch (Exception e) {
            System.err.println("Error initializing data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeAdminUser() {
        // Create admin user if it doesn't exist
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@bankx.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setRole(User.UserRole.ADMIN);
            admin.setEnabled(true);
            
            userRepository.save(admin);
            System.out.println("✅ Bank X Admin user created: admin / admin123");
        } else {
            System.out.println("✅ Bank X Admin user already exists");
        }
    }
    
    private void initializeSampleCustomers() {
        // Create customers with authentic South African names
        if (!userService.existsByEmail("thabo.mthembu@gmail.com")) {
            userService.createCustomerUser(
                "thabo.mthembu",
                "thabo.mthembu@gmail.com",
                "password123",
                "Thabo",
                "Mthembu",
                "+27821234567"
            );
            System.out.println("✅ Customer created: Thabo Mthembu (thabo.mthembu / password123)");
        }
        
        if (!userService.existsByEmail("nomsa.dlamini@outlook.com")) {
            userService.createCustomerUser(
                "nomsa.dlamini",
                "nomsa.dlamini@outlook.com",
                "password123",
                "Nomsa",
                "Dlamini",
                "+27834567890"
            );
            System.out.println("✅ Customer created: Nomsa Dlamini (nomsa.dlamini / password123)");
        }
        
        if (!userService.existsByEmail("sipho.nkomo@yahoo.com")) {
            userService.createCustomerUser(
                "sipho.nkomo",
                "sipho.nkomo@yahoo.com",
                "password123",
                "Sipho",
                "Nkomo",
                "+27798765432"
            );
            System.out.println("✅ Customer created: Sipho Nkomo (sipho.nkomo / password123)");
        }
        
        if (!userService.existsByEmail("lerato.mokoena@webmail.co.za")) {
            userService.createCustomerUser(
                "lerato.mokoena",
                "lerato.mokoena@webmail.co.za",
                "password123",
                "Lerato",
                "Mokoena",
                "+27765432109"
            );
            System.out.println("✅ Customer created: Lerato Mokoena (lerato.mokoena / password123)");
        }
        
        if (!userService.existsByEmail("mandla.van.der.merwe@gmail.com")) {
            userService.createCustomerUser(
                "mandla.vandermerwe",
                "mandla.van.der.merwe@gmail.com",
                "password123",
                "Mandla",
                "van der Merwe",
                "+27712345678"
            );
            System.out.println("✅ Customer created: Mandla van der Merwe (mandla.vandermerwe / password123)");
        }
    }
    
    private void printStartupInfo() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🏛️  BANK X APPLICATION STARTED SUCCESSFULLY");
        System.out.println("=".repeat(60));
        System.out.println("📍 Access Bank X at: http://localhost:9090/login");
        System.out.println("👤 Admin Login: admin / admin123");
        System.out.println("👤 Customer Logins:");
        System.out.println("   - thabo.mthembu / password123");
        System.out.println("   - nomsa.dlamini / password123");
        System.out.println("   - sipho.nkomo / password123");
        System.out.println("   - lerato.mokoena / password123");
        System.out.println("   - mandla.vandermerwe / password123");
        System.out.println("=".repeat(60));
        System.out.println("🔗 Bank Z Integration: http://localhost:9091");
        System.out.println("📊 Real South African customers and accounts created");
        System.out.println("💰 Each customer has Current and Savings accounts");
        System.out.println("🎁 R500 joining bonus added to Savings accounts");
        System.out.println("=".repeat(60) + "\n");
    }
}