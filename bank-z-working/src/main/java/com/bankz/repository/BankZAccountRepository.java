package com.bankz.repository;

import com.bankz.entity.BankZAccount;
import com.bankz.entity.BankZCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankZAccountRepository extends JpaRepository<BankZAccount, Long> {
    Optional<BankZAccount> findByAccountNumber(String accountNumber);
    List<BankZAccount> findByCustomer(BankZCustomer customer);
    List<BankZAccount> findByCustomerId(Long customerId);
    
    @Query("SELECT COUNT(a) FROM BankZAccount a")
    long countActiveAccounts();
}