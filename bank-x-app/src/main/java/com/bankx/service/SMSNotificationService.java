package com.bankx.service;

import com.bankx.config.TwilioConfig;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SMSNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SMSNotificationService.class);

    @Autowired
    private TwilioConfig twilioConfig;

    @Value("${twilio.phone.number:}")
    private String fromNumber;

    /**
     * Send SMS notification to a phone number
     * @param toNumber The recipient's phone number (must include country code, e.g., +27821234567)
     * @param message The message content to send
     * @return true if SMS was sent successfully, false otherwise
     */
    public boolean sendSMS(String toNumber, String message) {
        try {
            // Check if Twilio is configured
            if (!twilioConfig.isConfigured()) {
                logger.warn("Twilio is not configured. SMS will not be sent to: {}", toNumber);
                logger.info("Message would have been: {}", message);
                return false;
            }

            // Validate phone number format
            if (toNumber == null || toNumber.trim().isEmpty()) {
                logger.warn("Cannot send SMS: Phone number is null or empty");
                return false;
            }

            // Check if from number is configured
            if (fromNumber == null || fromNumber.trim().isEmpty()) {
                logger.warn("Twilio phone number not configured. Cannot send SMS to: {}", toNumber);
                return false;
            }

            // Ensure phone number starts with + for international format
            String formattedToNumber = toNumber.trim();
            if (!formattedToNumber.startsWith("+")) {
                // Assume South African number if no country code provided
                formattedToNumber = "+27" + formattedToNumber.replaceFirst("^0", "");
            }

            logger.info("Attempting to send SMS to: {} from: {}", formattedToNumber, fromNumber);

            Message twilioMessage = Message.creator(
                    new PhoneNumber(formattedToNumber),
                    new PhoneNumber(fromNumber),
                    message
            ).create();

            logger.info("SMS sent successfully. SID: {}, Status: {}", 
                       twilioMessage.getSid(), twilioMessage.getStatus());
            
            return true;

        } catch (Exception e) {
            logger.error("Failed to send SMS to {}: {}", toNumber, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send SMS with retry mechanism
     * @param toNumber The recipient's phone number
     * @param message The message content
     * @param maxRetries Maximum number of retry attempts
     * @return true if SMS was sent successfully, false otherwise
     */
    public boolean sendSMSWithRetry(String toNumber, String message, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            logger.info("SMS send attempt {} of {} to {}", attempt, maxRetries, toNumber);
            
            if (sendSMS(toNumber, message)) {
                return true;
            }
            
            if (attempt < maxRetries) {
                try {
                    // Wait before retry (exponential backoff)
                    Thread.sleep(1000 * attempt);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("SMS retry interrupted");
                    break;
                }
            }
        }
        
        logger.error("Failed to send SMS to {} after {} attempts", toNumber, maxRetries);
        return false;
    }

    /**
     * Format a transaction notification message
     * @param customerName The customer's name
     * @param transactionType The type of transaction (e.g., "Credit", "Debit", "Transfer")
     * @param amount The transaction amount
     * @param accountType The account type (e.g., "Current", "Savings")
     * @param balance The new account balance
     * @return Formatted SMS message
     */
    public String formatTransactionMessage(String customerName, String transactionType, 
                                         double amount, String accountType, double balance) {
        return String.format(
            "Hi %s, your %s account has been %s with R%.2f. New balance: R%.2f. Bank X",
            customerName, accountType, transactionType.toLowerCase(), amount, balance
        );
    }

    /**
     * Format a welcome message for new customers
     * @param customerName The customer's name
     * @param bonusAmount The welcome bonus amount
     * @return Formatted welcome SMS message
     */
    public String formatWelcomeMessage(String customerName, double bonusAmount) {
        return String.format(
            "Welcome to Bank X, %s! Your accounts have been created and you've received a R%.2f welcome bonus. Thank you for choosing Bank X!",
            customerName, bonusAmount
        );
    }

    /**
     * Format an OTP message
     * @param customerName The customer's name
     * @param otp The one-time password
     * @return Formatted OTP SMS message
     */
    public String formatOTPMessage(String customerName, String otp) {
        return String.format(
            "Hi %s, your Bank X OTP is: %s. This code expires in 5 minutes. Do not share this code with anyone.",
            customerName, otp
        );
    }
}