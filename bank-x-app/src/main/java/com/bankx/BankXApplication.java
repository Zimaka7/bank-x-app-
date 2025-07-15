package com.bankx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class BankXApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankXApplication.class, args);
        System.out.println("=================================================");
        System.out.println("Bank X Application Started Successfully!");
        System.out.println("API Documentation: http://localhost:8080/api");
        System.out.println("Health Check: http://localhost:8080/actuator/health");
        System.out.println("=================================================");
    }
}