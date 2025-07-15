package com.bankx.controller;

import com.bankx.dto.ApiResponse;
import com.bankx.entity.Notification;
import com.bankx.enums.NotificationType;
import com.bankx.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<Notification>>> getCustomerNotifications(@PathVariable Long customerId) {
        logger.info("Retrieving notifications for customer: {}", customerId);
        
        List<Notification> notifications = notificationService.getCustomerNotifications(customerId);
        
        return ResponseEntity.ok(ApiResponse.success("Customer notifications retrieved successfully", notifications));
    }

    @GetMapping("/customer/{customerId}/paged")
    public ResponseEntity<ApiResponse<Page<Notification>>> getCustomerNotificationsPaged(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("Retrieving paged notifications for customer: {} (page: {}, size: {})", 
                customerId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationService.getCustomerNotifications(customerId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Customer notifications retrieved successfully", notifications));
    }

    @GetMapping("/customer/{customerId}/type/{type}")
    public ResponseEntity<ApiResponse<List<Notification>>> getCustomerNotificationsByType(
            @PathVariable Long customerId,
            @PathVariable NotificationType type) {
        
        logger.info("Retrieving notifications for customer: {} of type: {}", customerId, type);
        
        List<Notification> notifications = notificationService.getCustomerNotificationsByType(customerId, type);
        
        return ResponseEntity.ok(ApiResponse.success("Customer notifications retrieved successfully", notifications));
    }
}