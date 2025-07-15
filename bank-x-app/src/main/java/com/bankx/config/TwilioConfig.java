package com.bankx.config;

import com.twilio.Twilio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class TwilioConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(TwilioConfig.class);
    
    @Value("${twilio.account.sid:}")
    private String accountSid;
    
    @Value("${twilio.auth.token:}")
    private String authToken;
    
    @Value("${twilio.phone.number:}")
    private String phoneNumber;

    @PostConstruct
    public void init() {
        logger.info("TwilioConfig @PostConstruct called");
        logger.info("Account SID: {}", accountSid);
        logger.info("Auth Token: {}", authToken != null ? authToken.substring(0, Math.min(authToken.length(), 8)) + "..." : "null");
        logger.info("Phone Number: {}", phoneNumber);
        
        if (accountSid != null && !accountSid.trim().isEmpty() && 
            authToken != null && !authToken.trim().isEmpty()) {
            
            logger.info("Initializing Twilio with Account SID: {}", 
                       accountSid.substring(0, Math.min(accountSid.length(), 10)) + "...");
            
            try {
                Twilio.init(accountSid, authToken);
                logger.info("Twilio initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize Twilio: {}", e.getMessage());
                logger.warn("SMS notifications will be disabled");
            }
        } else {
            logger.warn("Twilio credentials not provided. SMS notifications will be disabled.");
            logger.info("Account SID is: {}", accountSid);
            logger.info("Auth Token is: {}", authToken);
            logger.info("To enable SMS, set twilio.account.sid and twilio.auth.token in application.properties");
        }
    }

    // Getters and Setters
    public String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public boolean isConfigured() {
        return accountSid != null && !accountSid.trim().isEmpty() && 
               authToken != null && !authToken.trim().isEmpty();
    }
}