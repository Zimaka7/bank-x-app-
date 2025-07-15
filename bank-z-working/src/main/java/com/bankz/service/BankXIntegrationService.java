package com.bankz.service;

import com.bankz.dto.BankXTransactionRequest;
import com.bankz.entity.BankZTransaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BankXIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(BankXIntegrationService.class);
    
    @Value("${bankx.api.base-url}")
    private String bankXApiBaseUrl;
    
    @Value("${bankx.api.key}")
    private String bankXApiKey;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public BankXIntegrationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public boolean sendTransactionToBankX(BankZTransaction transaction, String customerId, String accountType) {
        try {
            String url = bankXApiBaseUrl + "/api/bankz/transaction";
            
            BankXTransactionRequest request = new BankXTransactionRequest(
                customerId,
                transaction.getTransactionType().name(),
                accountType,
                transaction.getAmount(),
                transaction.getReference()
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", bankXApiKey);
            
            HttpEntity<BankXTransactionRequest> entity = new HttpEntity<>(request, headers);
            
            logger.info("Sending transaction to Bank X: {}", transaction.getTransactionId());
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Transaction sent successfully to Bank X: {}", transaction.getTransactionId());
                return true;
            } else {
                logger.error("Failed to send transaction to Bank X. Status: {}", response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error sending transaction to Bank X: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public boolean sendBatchTransactionsToBankX(List<BankZTransaction> transactions, String customerId, String accountType) {
        try {
            String url = bankXApiBaseUrl + "/api/bankz/transactions";
            
            List<BankXTransactionRequest> requests = transactions.stream()
                .map(txn -> new BankXTransactionRequest(
                    customerId,
                    txn.getTransactionType().name(),
                    accountType,
                    txn.getAmount(),
                    txn.getReference()
                ))
                .toList();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", bankXApiKey);
            
            HttpEntity<List<BankXTransactionRequest>> entity = new HttpEntity<>(requests, headers);
            
            logger.info("Sending {} transactions to Bank X", transactions.size());
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Batch transactions sent successfully to Bank X");
                return true;
            } else {
                logger.error("Failed to send batch transactions to Bank X. Status: {}", response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error sending batch transactions to Bank X: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public boolean sendReconciliationToBankX(List<BankZTransaction> transactions, String customerId, String accountType) {
        try {
            String url = bankXApiBaseUrl + "/api/bankz/reconciliation";
            
            List<BankXTransactionRequest> requests = transactions.stream()
                .map(txn -> new BankXTransactionRequest(
                    customerId,
                    txn.getTransactionType().name(),
                    accountType,
                    txn.getAmount(),
                    txn.getReference()
                ))
                .toList();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", bankXApiKey);
            
            HttpEntity<List<BankXTransactionRequest>> entity = new HttpEntity<>(requests, headers);
            
            logger.info("Sending reconciliation data to Bank X for {} transactions", transactions.size());
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Reconciliation data sent successfully to Bank X");
                return true;
            } else {
                logger.error("Failed to send reconciliation data to Bank X. Status: {}", response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error sending reconciliation data to Bank X: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public boolean checkBankXStatus() {
        try {
            String url = bankXApiBaseUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error("Bank X health check failed: {}", e.getMessage());
            return false;
        }
    }
}