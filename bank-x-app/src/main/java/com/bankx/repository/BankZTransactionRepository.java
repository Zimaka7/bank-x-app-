package com.bankx.repository;

import com.bankx.entity.BankZTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BankZTransactionRepository extends JpaRepository<BankZTransaction, Long> {
    
    List<BankZTransaction> findByCustomerIdOrderByProcessedAtDesc(Long customerId);
    
    List<BankZTransaction> findByAccountNumberOrderByProcessedAtDesc(String accountNumber);
    
    @Query("SELECT bt FROM BankZTransaction bt WHERE bt.batchId = :batchId ORDER BY bt.processedAt DESC")
    List<BankZTransaction> findByBatchIdOrderByProcessedAtDesc(@Param("batchId") String batchId);
    
    @Query("SELECT bt FROM BankZTransaction bt WHERE bt.processedAt BETWEEN :startDate AND :endDate ORDER BY bt.processedAt DESC")
    List<BankZTransaction> findByProcessedAtBetweenOrderByProcessedAtDesc(@Param("startDate") LocalDateTime startDate, 
                                                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT bt FROM BankZTransaction bt WHERE bt.reference = :reference")
    List<BankZTransaction> findByReference(@Param("reference") String reference);
}