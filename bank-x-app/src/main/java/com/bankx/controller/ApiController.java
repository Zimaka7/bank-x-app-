package com.bankx.controller;

import com.bankx.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getApiInfo() {
        Map<String, Object> apiInfo = new HashMap<>();
        apiInfo.put("application", "Bank X Application");
        apiInfo.put("version", "1.0.0");
        apiInfo.put("description", "Banking Application for Bank X with PostgreSQL");
        apiInfo.put("timestamp", LocalDateTime.now());
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("customers", "/api/customers");
        endpoints.put("accounts", "/api/accounts");
        endpoints.put("transactions", "/api/transactions");
        endpoints.put("bankz", "/api/bankz");
        endpoints.put("notifications", "/api/notifications");
        endpoints.put("health", "/actuator/health");
        
        apiInfo.put("endpoints", endpoints);
        
        Map<String, String> features = new HashMap<>();
        features.put("customer_onboarding", "Register new customers with automatic account creation");
        features.put("account_management", "Current and Savings accounts with balance tracking");
        features.put("transactions", "Transfer, payment, and transaction history");
        features.put("bankz_integration", "Single and batch transaction processing for Bank Z");
        features.put("notifications", "Real-time notifications for all transactions");
        features.put("interest_calculation", "0.5% interest on Savings account credits");
        features.put("transaction_fees", "0.05% fee on all debits");
        features.put("joining_bonus", "R500 joining bonus for new customers");
        
        apiInfo.put("features", features);
        
        return ResponseEntity.ok(ApiResponse.success("Bank X API Information", apiInfo));
    }
}