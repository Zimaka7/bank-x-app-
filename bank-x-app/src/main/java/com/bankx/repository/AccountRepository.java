package com.bankx.repository;

import com.bankx.entity.Account;
import com.bankx.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Optional<Account> findByAccountNumber(String accountNumber);
    
    List<Account> findByCustomerId(Long customerId);
    
    @Query("SELECT a FROM Account a WHERE a.customer.id = :customerId AND a.accountType = :accountType")
    Optional<Account> findByCustomerIdAndAccountType(@Param("customerId") Long customerId, 
                                                    @Param("accountType") AccountType accountType);
    
    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.transactions WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithTransactions(@Param("accountNumber") String accountNumber);
    
    boolean existsByAccountNumber(String accountNumber);
}