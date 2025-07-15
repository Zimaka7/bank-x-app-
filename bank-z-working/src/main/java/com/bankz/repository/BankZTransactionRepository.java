package com.bankz.repository;

import com.bankz.entity.BankZAccount;
import com.bankz.entity.BankZTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankZTransactionRepository extends JpaRepository<BankZTransaction, Long> {
    Optional<BankZTransaction> findByTransactionId(String transactionId);
    List<BankZTransaction> findByAccount(BankZAccount account);
    List<BankZTransaction> findByAccountOrderByCreatedAtDesc(BankZAccount account);
    List<BankZTransaction> findByReference(String reference);
}