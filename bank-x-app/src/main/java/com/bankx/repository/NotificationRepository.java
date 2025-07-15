package com.bankx.repository;

import com.bankx.entity.Notification;
import com.bankx.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByCustomerIdOrderBySentAtDesc(Long customerId);
    
    Page<Notification> findByCustomerIdOrderBySentAtDesc(Long customerId, Pageable pageable);
    
    @Query("SELECT n FROM Notification n WHERE n.customer.id = :customerId AND n.type = :type ORDER BY n.sentAt DESC")
    List<Notification> findByCustomerIdAndTypeOrderBySentAtDesc(@Param("customerId") Long customerId, 
                                                               @Param("type") NotificationType type);
    
    @Query("SELECT n FROM Notification n WHERE n.sentAt BETWEEN :startDate AND :endDate ORDER BY n.sentAt DESC")
    List<Notification> findBySentAtBetweenOrderBySentAtDesc(@Param("startDate") LocalDateTime startDate, 
                                                           @Param("endDate") LocalDateTime endDate);
}