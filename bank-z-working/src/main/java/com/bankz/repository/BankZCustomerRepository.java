package com.bankz.repository;

import com.bankz.entity.BankZCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankZCustomerRepository extends JpaRepository<BankZCustomer, Long> {
    Optional<BankZCustomer> findByEmail(String email);
    Optional<BankZCustomer> findByIdNumber(String idNumber);
    boolean existsByEmail(String email);
    boolean existsByIdNumber(String idNumber);
}