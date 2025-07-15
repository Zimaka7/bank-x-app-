package com.bankz.repository;

import com.bankz.entity.BankZUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankZUserRepository extends JpaRepository<BankZUser, Long> {
    Optional<BankZUser> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsBySaIdNumber(String saIdNumber);
}