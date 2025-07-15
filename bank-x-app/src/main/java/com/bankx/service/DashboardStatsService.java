package com.bankx.service;

import com.bankx.entity.Account;
import com.bankx.entity.Customer;
import com.bankx.entity.Transaction;
import com.bankx.enums.TransactionType;
import com.bankx.repository.AccountRepository;
import com.bankx.repository.CustomerRepository;
import com.bankx.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardStatsService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Basic counts
        long totalCustomers = customerRepository.count();
        long totalAccounts = accountRepository.count();
        
        // Calculate total deposits (sum of all account balances)
        List<Account> allAccounts = accountRepository.findAll();
        BigDecimal totalDeposits = allAccounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Recent transactions
        List<Transaction> recentTransactions = transactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .limit(10)
                .toList();
        
        // Today's transactions
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        List<Transaction> todayTransactions = transactionRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(startOfDay, endOfDay);
        
        // Calculate today's transaction volume
        BigDecimal todayVolume = todayTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // New customers today
        List<Customer> todayCustomers = customerRepository.findAll().stream()
                .filter(customer -> customer.getCreatedAt().isAfter(startOfDay))
                .toList();
        
        // Yesterday's data for comparison
        LocalDateTime startOfYesterday = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime endOfYesterday = LocalDate.now().minusDays(1).atTime(23, 59, 59);
        List<Transaction> yesterdayTransactions = transactionRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(startOfYesterday, endOfYesterday);
        
        BigDecimal yesterdayVolume = yesterdayTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<Customer> yesterdayCustomers = customerRepository.findAll().stream()
                .filter(customer -> customer.getCreatedAt().isAfter(startOfYesterday) 
                        && customer.getCreatedAt().isBefore(endOfYesterday))
                .toList();
        
        // Calculate percentage changes
        double transactionCountChange = calculatePercentageChange(
                yesterdayTransactions.size(), todayTransactions.size());
        double volumeChange = calculatePercentageChange(yesterdayVolume, todayVolume);
        double customerChange = calculatePercentageChange(
                yesterdayCustomers.size(), todayCustomers.size());
        
        // This month's data
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<Customer> monthCustomers = customerRepository.findAll().stream()
                .filter(customer -> customer.getCreatedAt().isAfter(startOfMonth))
                .toList();
        
        List<Account> monthAccounts = accountRepository.findAll().stream()
                .filter(account -> account.getCreatedAt().isAfter(startOfMonth))
                .toList();
        
        // Last month's data for comparison
        LocalDateTime startOfLastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfLastMonth = LocalDate.now().withDayOfMonth(1).minusDays(1).atTime(23, 59, 59);
        
        List<Customer> lastMonthCustomers = customerRepository.findAll().stream()
                .filter(customer -> customer.getCreatedAt().isAfter(startOfLastMonth) 
                        && customer.getCreatedAt().isBefore(endOfLastMonth))
                .toList();
        
        List<Account> lastMonthAccounts = accountRepository.findAll().stream()
                .filter(account -> account.getCreatedAt().isAfter(startOfLastMonth) 
                        && account.getCreatedAt().isBefore(endOfLastMonth))
                .toList();
        
        double monthlyCustomerChange = calculatePercentageChange(
                lastMonthCustomers.size(), monthCustomers.size());
        double monthlyAccountChange = calculatePercentageChange(
                lastMonthAccounts.size(), monthAccounts.size());
        
        // This week's data
        LocalDateTime startOfWeek = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1).atStartOfDay();
        List<Transaction> weekTransactions = transactionRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(startOfWeek, LocalDateTime.now());
        
        BigDecimal weekVolume = weekTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Last week's data for comparison
        LocalDateTime startOfLastWeek = startOfWeek.minusWeeks(1);
        LocalDateTime endOfLastWeek = startOfWeek.minusDays(1);
        List<Transaction> lastWeekTransactions = transactionRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(startOfLastWeek, endOfLastWeek);
        
        BigDecimal lastWeekVolume = lastWeekTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        double weeklyVolumeChange = calculatePercentageChange(lastWeekVolume, weekVolume);
        
        // Populate stats map
        stats.put("totalCustomers", totalCustomers);
        stats.put("totalAccounts", totalAccounts);
        stats.put("totalDeposits", totalDeposits);
        stats.put("recentTransactions", recentTransactions);
        
        // Daily stats
        stats.put("dailyTransactionCount", todayTransactions.size());
        stats.put("dailyTransactionVolume", todayVolume);
        stats.put("dailyNewCustomers", todayCustomers.size());
        
        // Percentage changes
        stats.put("transactionCountChange", transactionCountChange);
        stats.put("volumeChange", volumeChange);
        stats.put("customerChange", customerChange);
        stats.put("monthlyCustomerChange", monthlyCustomerChange);
        stats.put("monthlyAccountChange", monthlyAccountChange);
        stats.put("weeklyVolumeChange", weeklyVolumeChange);
        
        // Security events (placeholder - could be enhanced with actual security monitoring)
        stats.put("securityEvents", 0);
        
        return stats;
    }
    
    private double calculatePercentageChange(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return newValue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return newValue.subtract(oldValue)
                .divide(oldValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
    
    private double calculatePercentageChange(int oldValue, int newValue) {
        if (oldValue == 0) {
            return newValue > 0 ? 100.0 : 0.0;
        }
        return ((double) (newValue - oldValue) / oldValue) * 100.0;
    }
}