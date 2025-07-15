package com.bankx.repository;

import com.bankx.entity.Transaction;
import com.bankx.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Optional<Transaction> findByTransactionId(String transactionId);
    
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    
    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.account.accountNumber = :accountNumber ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountNumberOrderByCreatedAtDesc(@Param("accountNumber") String accountNumber);
    
    @Query("SELECT t FROM Transaction t WHERE t.account.accountNumber = :accountNumber ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountNumberOrderByCreatedAtDesc(@Param("accountNumber") String accountNumber, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.account.customer.id = :customerId ORDER BY t.createdAt DESC")
    List<Transaction> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") Long customerId);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByCreatedAtBetweenOrderByCreatedAtDesc(@Param("startDate") LocalDateTime startDate, 
                                                               @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.transactionType = :transactionType ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountIdAndTransactionTypeOrderByCreatedAtDesc(@Param("accountId") Long accountId, 
                                                                          @Param("transactionType") TransactionType transactionType);
    
    List<Transaction> findAllByOrderByCreatedAtDesc();
}