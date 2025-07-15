package com.bankx.service;

import com.bankx.entity.Customer;
import com.bankx.entity.Notification;
import com.bankx.enums.NotificationType;
import com.bankx.exception.CustomerNotFoundException;
import com.bankx.repository.CustomerRepository;
import com.bankx.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SMSNotificationService smsNotificationService;

    @Value("${bankx.sms.enabled:true}")
    private boolean smsEnabled;

    public void sendNotification(Long customerId, String message, NotificationType type) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));

        Notification notification = new Notification(customer, message, type);
        notificationRepository.save(notification);

        logger.info("Notification sent to customer {}: {}", customerId, type);
        
        // Send actual notifications via SMS and other channels
        deliverNotification(customer, message, type);
    }

    @Transactional(readOnly = true)
    public List<Notification> getCustomerNotifications(Long customerId) {
        return notificationRepository.findByCustomerIdOrderBySentAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getCustomerNotifications(Long customerId, Pageable pageable) {
        return notificationRepository.findByCustomerIdOrderBySentAtDesc(customerId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Notification> getCustomerNotificationsByType(Long customerId, NotificationType type) {
        return notificationRepository.findByCustomerIdAndTypeOrderBySentAtDesc(customerId, type);
    }

    private void deliverNotification(Customer customer, String message, NotificationType type) {
        logger.info("=== NOTIFICATION DELIVERY ===");
        logger.info("To: {} ({})", customer.getFullName(), customer.getEmail());
        logger.info("Phone: {}", customer.getPhoneNumber());
        logger.info("Type: {}", type.getDisplayName());
        logger.info("Message: {}", message);
        
        // Send SMS notification if enabled and phone number is available
        if (smsEnabled && customer.getPhoneNumber() != null && !customer.getPhoneNumber().trim().isEmpty()) {
            boolean smsSent = smsNotificationService.sendSMSWithRetry(
                customer.getPhoneNumber(), 
                message, 
                3 // max retries
            );
            
            if (smsSent) {
                logger.info("SMS notification sent successfully to {}", customer.getPhoneNumber());
            } else {
                logger.warn("Failed to send SMS notification to {}", customer.getPhoneNumber());
            }
        } else {
            if (!smsEnabled) {
                logger.info("SMS notifications are disabled");
            } else {
                logger.warn("No valid phone number available for customer {}", customer.getId());
            }
        }
        
        logger.info("=============================");
        
        // Future integrations:
        // - Email service for email notifications
        // - Push notification service for mobile app notifications
        // - WhatsApp Business API for WhatsApp notifications
    }

    /**
     * Send transaction notification with formatted message
     */
    public void sendTransactionNotification(Long customerId, String transactionType, 
                                          double amount, String accountType, double newBalance) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));

        String message = smsNotificationService.formatTransactionMessage(
            customer.getFullName(), transactionType, amount, accountType, newBalance
        );

        NotificationType notificationType = determineNotificationType(transactionType);
        sendNotification(customerId, message, notificationType);
    }

    /**
     * Send welcome notification for new customers
     */
    public void sendWelcomeNotification(Long customerId, double bonusAmount) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));

        String message = smsNotificationService.formatWelcomeMessage(
            customer.getFullName(), bonusAmount
        );

        sendNotification(customerId, message, NotificationType.WELCOME);
    }

    /**
     * Send OTP notification
     */
    public void sendOTPNotification(Long customerId, String otp) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));

        String message = smsNotificationService.formatOTPMessage(
            customer.getFullName(), otp
        );

        // Send OTP notification
        sendNotification(customerId, message, NotificationType.OTP);
    }

    private NotificationType determineNotificationType(String transactionType) {
        switch (transactionType.toLowerCase()) {
            case "credit":
                return NotificationType.ACCOUNT_CREDIT;
            case "debit":
                return NotificationType.ACCOUNT_DEBIT;
            case "transfer":
                return NotificationType.TRANSFER;
            case "payment":
                return NotificationType.PAYMENT;
            case "interest":
                return NotificationType.INTEREST;
            case "fee":
                return NotificationType.FEE;
            default:
                return NotificationType.TRANSACTION;
        }
    }
}