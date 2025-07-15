package com.bankx.controller;

import com.bankx.service.NotificationService;
import com.bankx.service.SMSNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sms")
public class SMSTestController {

    private static final Logger logger = LoggerFactory.getLogger(SMSTestController.class);

    @Autowired
    private SMSNotificationService smsNotificationService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Test endpoint to send a simple SMS
     * POST /api/sms/test
     * Body: {"phoneNumber": "+27821234567", "message": "Test message"}
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testSMS(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String phoneNumber = request.get("phoneNumber");
            String message = request.get("message");
            
            if (phoneNumber == null || message == null) {
                response.put("success", false);
                response.put("error", "phoneNumber and message are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            logger.info("Testing SMS to: {} with message: {}", phoneNumber, message);
            
            boolean sent = smsNotificationService.sendSMS(phoneNumber, message);
            
            response.put("success", sent);
            response.put("phoneNumber", phoneNumber);
            response.put("message", message);
            
            if (sent) {
                response.put("status", "SMS sent successfully");
            } else {
                response.put("status", "Failed to send SMS");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error testing SMS: ", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Test endpoint to send a transaction notification SMS to a customer
     * POST /api/sms/transaction-test
     * Body: {"customerId": 1, "transactionType": "credit", "amount": 100.0, "accountType": "Savings", "newBalance": 600.0}
     */
    @PostMapping("/transaction-test")
    public ResponseEntity<Map<String, Object>> testTransactionSMS(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long customerId = Long.valueOf(request.get("customerId").toString());
            String transactionType = (String) request.get("transactionType");
            Double amount = Double.valueOf(request.get("amount").toString());
            String accountType = (String) request.get("accountType");
            Double newBalance = Double.valueOf(request.get("newBalance").toString());
            
            logger.info("Testing transaction SMS for customer: {}", customerId);
            
            notificationService.sendTransactionNotification(customerId, transactionType, amount, accountType, newBalance);
            
            response.put("success", true);
            response.put("customerId", customerId);
            response.put("transactionType", transactionType);
            response.put("amount", amount);
            response.put("accountType", accountType);
            response.put("newBalance", newBalance);
            response.put("status", "Transaction notification sent");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error testing transaction SMS: ", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Test endpoint to send a welcome SMS to a customer
     * POST /api/sms/welcome-test
     * Body: {"customerId": 1, "bonusAmount": 500.0}
     */
    @PostMapping("/welcome-test")
    public ResponseEntity<Map<String, Object>> testWelcomeSMS(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long customerId = Long.valueOf(request.get("customerId").toString());
            Double bonusAmount = Double.valueOf(request.get("bonusAmount").toString());
            
            logger.info("Testing welcome SMS for customer: {}", customerId);
            
            notificationService.sendWelcomeNotification(customerId, bonusAmount);
            
            response.put("success", true);
            response.put("customerId", customerId);
            response.put("bonusAmount", bonusAmount);
            response.put("status", "Welcome notification sent");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error testing welcome SMS: ", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Test endpoint to send an OTP SMS to a customer
     * POST /api/sms/otp-test
     * Body: {"customerId": 1, "otp": "123456"}
     */
    @PostMapping("/otp-test")
    public ResponseEntity<Map<String, Object>> testOTPSMS(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long customerId = Long.valueOf(request.get("customerId").toString());
            String otp = (String) request.get("otp");
            
            logger.info("Testing OTP SMS for customer: {}", customerId);
            
            notificationService.sendOTPNotification(customerId, otp);
            
            response.put("success", true);
            response.put("customerId", customerId);
            response.put("otp", otp);
            response.put("status", "OTP notification sent");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error testing OTP SMS: ", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}