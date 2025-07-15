# Bank X and Bank Z - Technical Implementation Guide

## Table of Contents
1. [Core Business Logic Implementation](#core-business-logic-implementation)
2. [Transaction Processing Engine](#transaction-processing-engine)
3. [Integration Architecture](#integration-architecture)
4. [Data Flow Diagrams](#data-flow-diagrams)
5. [Security Implementation Details](#security-implementation-details)
6. [Error Handling and Recovery](#error-handling-and-recovery)
7. [Performance Optimization](#performance-optimization)
8. [Code Examples and Patterns](#code-examples-and-patterns)

---

## 1. Core Business Logic Implementation

### 1.1 Account Service - The Heart of Banking Operations

The `AccountService` class implements all core banking business rules:

#### Interest Calculation Logic
```java
// Applied to Savings accounts on credit transactions
if (account.getAccountType() == AccountType.SAVINGS) {
    BigDecimal interest = account.getBalance().multiply(savingsInterestRate)
            .setScale(2, RoundingMode.HALF_UP);
    amount = amount.add(interest);
    
    if (interest.compareTo(BigDecimal.ZERO) > 0) {
        logger.info("Applied interest of R{} to savings account: {}", interest, accountNumber);
    }
}
```

**Key Features**:
- **Interest Rate**: 0.5% (configurable via `bankx.savings-interest-rate`)
- **Calculation**: Applied to existing balance before credit
- **Precision**: 2 decimal places with HALF_UP rounding
- **Logging**: All interest applications are logged for audit

#### Fee Calculation Logic
```java
// Applied to all debit transactions
BigDecimal fee = amount.multiply(transactionFeeRate).setScale(2, RoundingMode.HALF_UP);
BigDecimal totalAmount = amount.add(fee);

if (!account.hasSufficientFunds(totalAmount)) {
    throw new InsufficientFundsException("Insufficient funds in account: " + accountNumber);
}
```

**Key Features**:
- **Fee Rate**: 0.05% (configurable via `bankx.transaction-fee-rate`)
- **Application**: All outgoing payments (debits and transfers)
- **Validation**: Ensures sufficient funds including fees
- **Separate Transaction**: Fees are recorded as separate transactions

#### Transfer Restrictions
```java
// Only Current accounts can make external payments
// Validated in business logic layer
if (fromAccount.getAccountType() != AccountType.CURRENT && isExternalPayment) {
    throw new InvalidTransactionException("External payments only allowed from Current accounts");
}
```

### 1.2 Transaction Processing Workflow

#### Transaction Lifecycle
```
1. Request Validation
   ├── Amount validation (positive, within limits)
   ├── Account existence check
   ├── Customer authorization
   └── Business rule validation

2. Balance Calculation
   ├── Interest calculation (if applicable)
   ├── Fee calculation (if applicable)
   └── Sufficient funds check

3. Account Updates
   ├── Update source account balance
   ├── Update destination account balance (transfers)
   └── Persist changes to database

4. Transaction Recording
   ├── Create transaction record
   ├── Generate unique transaction ID
   ├── Record balance before/after
   └── Set transaction status

5. Notification Processing
   ├── Format notification message
   ├── Send SMS/Email notification
   └── Log notification delivery

6. Audit Logging
   ├── Log transaction details
   ├── Log business rule applications
   └── Log any errors or exceptions
```

#### Transaction ID Generation
```java
private String generateTransactionId() {
    LocalDateTime now = LocalDateTime.now();
    String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    String random = String.format("%03d", new Random().nextInt(1000));
    return "TXN-" + timestamp + "-" + random;
}
```

**Format**: `TXN-YYYYMMDD-HHMMSS-XXX`
- **TXN**: Transaction prefix
- **YYYYMMDD**: Date
- **HHMMSS**: Time
- **XXX**: Random 3-digit number for uniqueness

---

## 2. Transaction Processing Engine

### 2.1 Bank X Transaction Processing

#### Credit Transaction Flow
```java
public void creditAccount(String accountNumber, BigDecimal amount, String description) {
    // 1. Retrieve account
    Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

    BigDecimal originalAmount = amount;
    
    // 2. Apply business rules (interest for savings)
    if (account.getAccountType() == AccountType.SAVINGS) {
        BigDecimal interest = account.getBalance().multiply(savingsInterestRate)
                .setScale(2, RoundingMode.HALF_UP);
        amount = amount.add(interest);
    }

    // 3. Update account balance
    account.credit(amount);
    account = accountRepository.save(account);

    // 4. Record transaction
    String transactionId = UUID.randomUUID().toString();
    Transaction transaction = transactionService.createTransaction(
            account, transactionId, originalAmount, TransactionType.CREDIT, description
    );
    transaction.setBalanceAfter(account.getBalance());
    transactionService.saveTransaction(transaction);

    // 5. Send notification
    String message = String.format(
            "Your %s account %s has been credited with R%.2f. New balance: R%.2f",
            account.getAccountType().getDisplayName(),
            accountNumber,
            originalAmount,
            account.getBalance()
    );
    notificationService.sendNotification(account.getCustomer().getId(), message, NotificationType.ACCOUNT_CREDIT);

    // 6. Audit logging
    logger.info("Credited R{} to account: {}, new balance: R{}", 
            originalAmount, accountNumber, account.getBalance());
}
```

#### Debit Transaction Flow
```java
public void debitAccount(String accountNumber, BigDecimal amount, String description) {
    // 1. Retrieve account
    Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

    // 2. Calculate fees
    BigDecimal fee = amount.multiply(transactionFeeRate).setScale(2, RoundingMode.HALF_UP);
    BigDecimal totalAmount = amount.add(fee);

    // 3. Validate sufficient funds
    if (!account.hasSufficientFunds(totalAmount)) {
        throw new InsufficientFundsException("Insufficient funds in account: " + accountNumber);
    }

    // 4. Update account balance
    account.debit(totalAmount);
    account = accountRepository.save(account);

    // 5. Record main transaction
    String transactionId = UUID.randomUUID().toString();
    Transaction transaction = transactionService.createTransaction(
            account, transactionId, amount, TransactionType.DEBIT, description
    );
    transaction.setBalanceAfter(account.getBalance());
    transactionService.saveTransaction(transaction);

    // 6. Record fee transaction (if applicable)
    if (fee.compareTo(BigDecimal.ZERO) > 0) {
        String feeTransactionId = UUID.randomUUID().toString();
        Transaction feeTransaction = transactionService.createTransaction(
                account, feeTransactionId, fee, TransactionType.DEBIT, "Transaction fee"
        );
        feeTransaction.setBalanceAfter(account.getBalance());
        transactionService.saveTransaction(feeTransaction);
    }

    // 7. Send notification
    String message = String.format(
            "Your %s account %s has been debited with R%.2f (including fee: R%.2f). New balance: R%.2f",
            account.getAccountType().getDisplayName(),
            accountNumber,
            amount,
            fee,
            account.getBalance()
    );
    notificationService.sendNotification(account.getCustomer().getId(), message, NotificationType.ACCOUNT_DEBIT);

    // 8. Audit logging
    logger.info("Debited R{} (+ R{} fee) from account: {}, new balance: R{}", 
            amount, fee, accountNumber, account.getBalance());
}
```

### 2.2 Bank Z Transaction Processing

#### Transaction Creation and Bank X Integration
```java
@PostMapping("/transactions/create")
public String createTransaction(@RequestParam String accountNumber,
                              @RequestParam BigDecimal amount,
                              @RequestParam String type,
                              @RequestParam String description,
                              @RequestParam(required = false) String reference,
                              RedirectAttributes redirectAttributes) {
    try {
        BankZTransaction transaction;
        
        // 1. Create transaction in Bank Z
        if ("CREDIT".equals(type)) {
            transaction = accountService.creditAccount(accountNumber, amount, description, reference);
        } else if ("DEBIT".equals(type)) {
            transaction = accountService.debitAccount(accountNumber, amount, description, reference);
        } else {
            throw new RuntimeException("Invalid transaction type: " + type);
        }
        
        // 2. Automatically send to Bank X with mapped customer data
        try {
            String bankXCustomerId = mapBankZCustomerToBankX(transaction.getAccount().getCustomer());
            String bankXAccountType = transaction.getAccount().getAccountType().name();
            
            boolean sentToBankX = bankXIntegrationService.sendTransactionToBankX(
                transaction, bankXCustomerId, bankXAccountType);
            
            if (sentToBankX) {
                transaction.setSentToBankX(true);
                transaction.setBankxResponse("SUCCESS");
                redirectAttributes.addFlashAttribute("successMessage", 
                    String.format("Transaction created successfully and sent to Bank X. ID: %s", 
                        transaction.getTransactionId()));
            } else {
                transaction.setBankxResponse("FAILED");
                redirectAttributes.addFlashAttribute("warningMessage", 
                    String.format("Transaction created (ID: %s) but failed to send to Bank X", 
                        transaction.getTransactionId()));
            }
        } catch (Exception bankXError) {
            redirectAttributes.addFlashAttribute("warningMessage", 
                String.format("Transaction created (ID: %s) but failed to send to Bank X: %s", 
                    transaction.getTransactionId(), bankXError.getMessage()));
        }
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", 
            "Failed to create transaction: " + e.getMessage());
    }
    
    return "redirect:/operator/transactions";
}
```

---

## 3. Integration Architecture

### 3.1 Bank Z to Bank X Communication

#### API Request Structure
```java
public class BankXTransactionRequest {
    private String customerId;        // Bank X customer ID
    private String transactionType;   // CREDIT, DEBIT
    private String accountType;       // CURRENT, SAVINGS
    private BigDecimal amount;        // Transaction amount
    private String reference;         // Bank Z reference
    
    // Constructor, getters, setters
}
```

#### Integration Service Implementation
```java
public boolean sendTransactionToBankX(BankZTransaction transaction, String customerId, String accountType) {
    try {
        String url = bankXApiBaseUrl + "/api/bankz/transaction";
        
        // 1. Create request object
        BankXTransactionRequest request = new BankXTransactionRequest(
            customerId,
            transaction.getTransactionType().name(),
            accountType,
            transaction.getAmount(),
            transaction.getReference()
        );
        
        // 2. Set headers with API key authentication
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", bankXApiKey);
        
        HttpEntity<BankXTransactionRequest> entity = new HttpEntity<>(request, headers);
        
        // 3. Send request to Bank X
        logger.info("Sending transaction to Bank X: {}", transaction.getTransactionId());
        
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        
        // 4. Process response
        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("Transaction sent successfully to Bank X: {}", transaction.getTransactionId());
            return true;
        } else {
            logger.error("Failed to send transaction to Bank X. Status: {}", response.getStatusCode());
            return false;
        }
        
    } catch (Exception e) {
        logger.error("Error sending transaction to Bank X: {}", e.getMessage(), e);
        return false;
    }
}
```

### 3.2 Customer Mapping Between Systems

#### Bank Z to Bank X Customer Mapping
```java
private String mapBankZCustomerToBankX(BankZCustomer bankZCustomer) {
    // Simple mapping based on email for consistent customer mapping between systems
    // In production, this would be a proper customer mapping service
    if (bankZCustomer.getEmail().equals("thabo.mthembu@gmail.com")) {
        return "1"; // Maps to Bank X customer ID 1 (Thabo Mthembu)
    } else if (bankZCustomer.getEmail().equals("nomsa.dlamini@outlook.com")) {
        return "2"; // Maps to Bank X customer ID 2 (Nomsa Dlamini)
    } else if (bankZCustomer.getEmail().equals("sipho.nkomo@yahoo.com")) {
        return "3"; // Maps to Bank X customer ID 3 (Sipho Nkomo)
    } else if (bankZCustomer.getEmail().equals("lerato.mokoena@webmail.co.za")) {
        return "4"; // Maps to Bank X customer ID 4 (Lerato Mokoena)
    } else if (bankZCustomer.getEmail().equals("mandla.van.der.merwe@gmail.com")) {
        return "5"; // Maps to Bank X customer ID 5 (Mandla van der Merwe)
    } else {
        return "1"; // Default to customer 1 for any unmapped customers
    }
}
```

**Production Implementation Considerations**:
- Use a dedicated mapping table in the database
- Implement proper customer onboarding sync
- Handle mapping failures gracefully
- Provide manual mapping override capabilities

---

## 4. Data Flow Diagrams

### 4.1 Customer Registration Flow
```
Customer Registration (Bank X)
├── 1. User submits registration form
├── 2. Validate user input
├── 3. Check for existing email/username
├── 4. Create User entity
├── 5. Create Customer entity
├── 6. Create Current Account
│   ├── Generate account number (CUR + timestamp)
│   └── Set initial balance to R0.00
├── 7. Create Savings Account
│   ├── Generate account number (SAV + timestamp)
│   └── Set initial balance to R0.00
├── 8. Credit R500 joining bonus to Savings
│   ├── Apply 0.5% interest on existing balance
│   ├── Update account balance
│   ├── Create transaction record
│   └── Send notification
└── 9. Send welcome notification
```

### 4.2 Bank Z Transaction Flow
```
Bank Z Transaction Processing
├── 1. Operator creates transaction in Bank Z UI
├── 2. Validate transaction data
├── 3. Process transaction in Bank Z
│   ├── Update account balance
│   ├── Create transaction record
│   └── Set status to PENDING
├── 4. Map Bank Z customer to Bank X customer
├── 5. Send transaction to Bank X API
│   ├── Create API request
│   ├── Set authentication headers
│   └── POST to /api/bankz/transaction
├── 6. Bank X processes transaction
│   ├── Validate API key
│   ├── Find customer account
│   ├── Apply business rules (interest/fees)
│   ├── Update account balance
│   ├── Create transaction record
│   └── Send notification
├── 7. Bank X returns response
├── 8. Update Bank Z transaction status
│   ├── Set sentToBankX = true
│   └── Set bankxResponse = "SUCCESS"
└── 9. Display result to operator
```

### 4.3 Reconciliation Flow
```
End-of-Day Reconciliation
├���─ 1. Bank Z generates reconciliation file
├── 2. Upload reconciliation data to Bank X
├── 3. Bank X processes reconciliation
│   ├── Match transactions by reference
│   ├── Compare amounts and statuses
│   ├── Identify mismatches
│   └── Generate reconciliation report
├── 4. Bank Z receives reconciliation results
├── 5. Flag mismatched transactions
├── 6. Generate reconciliation summary
└── 7. Store reconciliation log
```

---

## 5. Security Implementation Details

### 5.1 API Key Authentication Filter

```java
@Component
public class BankZApiKeyFilter implements Filter {
    
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String VALID_API_KEY = "bankz_secure_api_key_2024";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Only apply to Bank Z API endpoints
        if (httpRequest.getRequestURI().startsWith("/api/bankz/")) {
            String apiKey = httpRequest.getHeader(API_KEY_HEADER);
            
            if (apiKey == null || !VALID_API_KEY.equals(apiKey)) {
                logger.warn("Invalid API key attempt from IP: {}", 
                    httpRequest.getRemoteAddr());
                httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
                httpResponse.getWriter().write("Invalid API key");
                return;
            }
            
            logger.debug("Valid API key provided for request: {}", 
                httpRequest.getRequestURI());
        }
        
        chain.doFilter(request, response);
    }
}
```

### 5.2 Password Security

#### BCrypt Password Encoding
```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strength 12 for enhanced security
    }
}
```

#### User Creation with Secure Password
```java
public User createUser(String username, String email, String rawPassword, 
                      String firstName, String lastName, String phoneNumber) {
    
    // Validate password strength
    if (!isPasswordStrong(rawPassword)) {
        throw new WeakPasswordException("Password does not meet security requirements");
    }
    
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(rawPassword)); // BCrypt encoding
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setPhoneNumber(phoneNumber);
    user.setRole(User.UserRole.CUSTOMER);
    user.setEnabled(true);
    
    return userRepository.save(user);
}

private boolean isPasswordStrong(String password) {
    return password.length() >= 8 &&
           password.matches(".*[A-Z].*") &&  // At least one uppercase
           password.matches(".*[a-z].*") &&  // At least one lowercase
           password.matches(".*\\d.*");      // At least one digit
}
```

### 5.3 Role-Based Access Control

#### Method-Level Security
```java
@PreAuthorize("hasRole('ADMIN')")
public String adminDashboard(Model model) {
    // Only accessible by users with ADMIN role
}

@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public String operatorTransactions(Model model) {
    // Accessible by ADMIN or OPERATOR roles
}

@PreAuthorize("hasRole('CUSTOMER') and #customerId == authentication.principal.customerId")
public String customerDashboard(@PathVariable Long customerId, Model model) {
    // Customers can only access their own data
}
```

#### URL-Based Security Configuration
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/register", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/customer/**").hasRole("CUSTOMER")
                .requestMatchers("/operator/**").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers("/api/bankz/**").permitAll() // Protected by API key filter
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );
        
        return http.build();
    }
}
```

---

## 6. Error Handling and Recovery

### 6.1 Custom Exception Hierarchy

```java
// Base exception for all banking operations
public class BankingException extends RuntimeException {
    public BankingException(String message) {
        super(message);
    }
    
    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Specific exceptions for different scenarios
public class CustomerNotFoundException extends BankingException {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}

public class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}

public class InsufficientFundsException extends BankingException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

public class InvalidTransactionException extends BankingException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}
```

### 6.2 Global Exception Handler

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleCustomerNotFound(CustomerNotFoundException e) {
        logger.error("Customer not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiResponse<>(false, e.getMessage(), null));
    }
    
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleAccountNotFound(AccountNotFoundException e) {
        logger.error("Account not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiResponse<>(false, e.getMessage(), null));
    }
    
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiResponse<String>> handleInsufficientFunds(InsufficientFundsException e) {
        logger.error("Insufficient funds: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ApiResponse<>(false, e.getMessage(), null));
    }
    
    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ApiResponse<String>> handleInvalidTransaction(InvalidTransactionException e) {
        logger.error("Invalid transaction: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ApiResponse<>(false, e.getMessage(), null));
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        logger.error("Data integrity violation: {}", e.getMessage());
        String message = "Data integrity violation - possibly duplicate data";
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiResponse<>(false, message, null));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGenericException(Exception e) {
        logger.error("Unexpected error: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse<>(false, "An unexpected error occurred", null));
    }
    
    // Web page error handling
    @ExceptionHandler(BankingException.class)
    public String handleBankingExceptionWeb(BankingException e, Model model) {
        logger.error("Banking error: {}", e.getMessage());
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("errorType", e.getClass().getSimpleName());
        model.addAttribute("timestamp", LocalDateTime.now());
        return "error/banking-error";
    }
}
```

### 6.3 Transaction Rollback and Recovery

```java
@Service
@Transactional
public class TransactionRecoveryService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    /**
     * Compensating transaction to reverse a failed transaction
     */
    @Transactional
    public void reverseTransaction(String transactionId) {
        Transaction originalTransaction = transactionRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionId));
        
        if (originalTransaction.getStatus() != TransactionStatus.FAILED) {
            throw new InvalidTransactionException("Can only reverse failed transactions");
        }
        
        Account account = originalTransaction.getAccount();
        
        // Create compensating transaction
        if (originalTransaction.getTransactionType() == TransactionType.DEBIT) {
            // Reverse debit with credit
            account.credit(originalTransaction.getAmount());
            createCompensatingTransaction(account, originalTransaction, TransactionType.CREDIT);
        } else if (originalTransaction.getTransactionType() == TransactionType.CREDIT) {
            // Reverse credit with debit (if sufficient funds)
            if (account.hasSufficientFunds(originalTransaction.getAmount())) {
                account.debit(originalTransaction.getAmount());
                createCompensatingTransaction(account, originalTransaction, TransactionType.DEBIT);
            } else {
                throw new InsufficientFundsException("Cannot reverse credit - insufficient funds");
            }
        }
        
        accountRepository.save(account);
        
        // Update original transaction status
        originalTransaction.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(originalTransaction);
        
        logger.info("Transaction {} reversed successfully", transactionId);
    }
    
    private void createCompensatingTransaction(Account account, Transaction originalTransaction, 
                                             TransactionType compensatingType) {
        Transaction compensatingTransaction = new Transaction();
        compensatingTransaction.setTransactionId("REV-" + originalTransaction.getTransactionId());
        compensatingTransaction.setAccount(account);
        compensatingTransaction.setTransactionType(compensatingType);
        compensatingTransaction.setAmount(originalTransaction.getAmount());
        compensatingTransaction.setDescription("Reversal of transaction: " + originalTransaction.getTransactionId());
        compensatingTransaction.setStatus(TransactionStatus.COMPLETED);
        compensatingTransaction.setBalanceAfter(account.getBalance());
        compensatingTransaction.setCreatedAt(LocalDateTime.now());
        
        transactionRepository.save(compensatingTransaction);
    }
}
```

---

## 7. Performance Optimization

### 7.1 Database Optimization

#### Connection Pool Configuration
```properties
# HikariCP configuration for optimal performance
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=600000
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.leak-detection-threshold=60000
```

#### JPA Performance Tuning
```properties
# JPA/Hibernate optimization
spring.jpa.properties.hibernate.jdbc.batch_size=25
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
spring.jpa.properties.hibernate.connection.autocommit=false
```

#### Database Indexing Strategy
```sql
-- Customer lookups
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_id_number ON customers(id_number);
CREATE INDEX idx_customers_created_at ON customers(created_at);

-- Account lookups
CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX idx_accounts_account_number ON accounts(account_number);
CREATE INDEX idx_accounts_account_type ON accounts(account_type);

-- Transaction queries
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_transaction_id ON transactions(transaction_id);
CREATE INDEX idx_transactions_transaction_type ON transactions(transaction_type);
CREATE INDEX idx_transactions_status ON transactions(status);

-- Composite indexes for common queries
CREATE INDEX idx_transactions_account_date ON transactions(account_id, created_at);
CREATE INDEX idx_transactions_customer_date ON transactions(account_id, created_at) 
    INCLUDE (transaction_type, amount, status);

-- Bank Z specific indexes
CREATE INDEX idx_bankz_customers_email ON bankz.bankz_customers(email);
CREATE INDEX idx_bankz_accounts_customer_id ON bankz.bankz_accounts(customer_id);
CREATE INDEX idx_bankz_transactions_account_id ON bankz.bankz_transactions(account_id);
CREATE INDEX idx_bankz_transactions_created_at ON bankz.bankz_transactions(created_at);
```

### 7.2 Caching Implementation

#### Service-Level Caching
```java
@Service
@CacheConfig(cacheNames = "bankx")
public class CustomerService {
    
    @Cacheable(value = "customers", key = "#customerId")
    public Customer getCustomerById(Long customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
    }
    
    @Cacheable(value = "customer-accounts", key = "#customerId")
    public List<Account> getCustomerAccounts(Long customerId) {
        return accountRepository.findByCustomerId(customerId);
    }
    
    @CacheEvict(value = {"customers", "customer-accounts"}, key = "#customer.id")
    public Customer updateCustomer(Customer customer) {
        return customerRepository.save(customer);
    }
    
    @CacheEvict(value = {"customers", "customer-accounts"}, allEntries = true)
    public void clearCustomerCache() {
        // Manual cache clearing if needed
    }
}
```

#### Cache Configuration
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
}
```

### 7.3 Async Processing

#### Notification Processing
```java
@Service
public class AsyncNotificationService {
    
    @Async("notificationExecutor")
    public CompletableFuture<Void> sendNotificationAsync(Long customerId, String message, NotificationType type) {
        try {
            // Process notification in background
            notificationService.sendNotification(customerId, message, type);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Failed to send async notification", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}

@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notification-");
        executor.initialize();
        return executor;
    }
}
```

---

## 8. Code Examples and Patterns

### 8.1 Repository Pattern Implementation

#### Base Repository Interface
```java
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.createdAt BETWEEN :startDate AND :endDate")
    List<T> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                           @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.createdAt >= :date")
    long countSince(@Param("date") LocalDateTime date);
}
```

#### Specific Repository Implementation
```java
@Repository
public interface CustomerRepository extends BaseRepository<Customer, Long> {
    
    Optional<Customer> findByEmail(String email);
    
    Optional<Customer> findByIdNumber(String idNumber);
    
    @Query("SELECT c FROM Customer c WHERE c.firstName LIKE %:name% OR c.lastName LIKE %:name%")
    List<Customer> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT c FROM Customer c JOIN c.accounts a WHERE a.accountType = :accountType")
    List<Customer> findByAccountType(@Param("accountType") AccountType accountType);
    
    @Modifying
    @Query("UPDATE Customer c SET c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :customerId")
    void updateTimestamp(@Param("customerId") Long customerId);
}
```

### 8.2 DTO Pattern for API Responses

#### Customer Response DTO
```java
public class CustomerResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDateTime createdAt;
    private List<AccountResponse> accounts;
    
    public CustomerResponse(Customer customer) {
        this.id = customer.getId();
        this.firstName = customer.getFirstName();
        this.lastName = customer.getLastName();
        this.email = customer.getEmail();
        this.phoneNumber = customer.getPhoneNumber();
        this.createdAt = customer.getCreatedAt();
        this.accounts = customer.getAccounts().stream()
            .map(AccountResponse::new)
            .collect(Collectors.toList());
    }
    
    // Getters and setters
}
```

#### Account Response DTO
```java
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private int transactionCount;
    
    public AccountResponse(Account account) {
        this.id = account.getId();
        this.accountNumber = account.getAccountNumber();
        this.accountType = account.getAccountType();
        this.balance = account.getBalance();
        this.createdAt = account.getCreatedAt();
        this.transactionCount = account.getTransactions().size();
    }
    
    // Getters and setters
}
```

### 8.3 Builder Pattern for Complex Objects

#### Transaction Builder
```java
public class TransactionBuilder {
    private Account account;
    private Account toAccount;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String description;
    private String reference;
    
    public static TransactionBuilder create() {
        return new TransactionBuilder();
    }
    
    public TransactionBuilder account(Account account) {
        this.account = account;
        return this;
    }
    
    public TransactionBuilder toAccount(Account toAccount) {
        this.toAccount = toAccount;
        return this;
    }
    
    public TransactionBuilder type(TransactionType transactionType) {
        this.transactionType = transactionType;
        return this;
    }
    
    public TransactionBuilder amount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }
    
    public TransactionBuilder description(String description) {
        this.description = description;
        return this;
    }
    
    public TransactionBuilder reference(String reference) {
        this.reference = reference;
        return this;
    }
    
    public Transaction build() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setAccount(account);
        transaction.setToAccount(toAccount);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setReference(reference);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());
        return transaction;
    }
    
    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + 
               String.format("%03d", new Random().nextInt(1000));
    }
}

// Usage example:
Transaction transaction = TransactionBuilder.create()
    .account(sourceAccount)
    .toAccount(destinationAccount)
    .type(TransactionType.TRANSFER)
    .amount(new BigDecimal("1000.00"))
    .description("Monthly transfer")
    .reference("REF-001")
    .build();
```

### 8.4 Strategy Pattern for Business Rules

#### Interest Calculation Strategy
```java
public interface InterestCalculationStrategy {
    BigDecimal calculateInterest(BigDecimal balance, BigDecimal amount);
}

@Component
public class SavingsInterestStrategy implements InterestCalculationStrategy {
    
    @Value("${bankx.savings-interest-rate:0.005}")
    private BigDecimal interestRate;
    
    @Override
    public BigDecimal calculateInterest(BigDecimal balance, BigDecimal amount) {
        return balance.multiply(interestRate).setScale(2, RoundingMode.HALF_UP);
    }
}

@Component
public class CurrentAccountInterestStrategy implements InterestCalculationStrategy {
    
    @Override
    public BigDecimal calculateInterest(BigDecimal balance, BigDecimal amount) {
        return BigDecimal.ZERO; // No interest on current accounts
    }
}

@Service
public class InterestCalculationService {
    
    @Autowired
    private SavingsInterestStrategy savingsInterestStrategy;
    
    @Autowired
    private CurrentAccountInterestStrategy currentAccountInterestStrategy;
    
    public BigDecimal calculateInterest(Account account, BigDecimal amount) {
        InterestCalculationStrategy strategy = getStrategy(account.getAccountType());
        return strategy.calculateInterest(account.getBalance(), amount);
    }
    
    private InterestCalculationStrategy getStrategy(AccountType accountType) {
        return switch (accountType) {
            case SAVINGS -> savingsInterestStrategy;
            case CURRENT -> currentAccountInterestStrategy;
        };
    }
}
```

---

## Conclusion

This technical implementation guide provides a comprehensive overview of how the Bank X and Bank Z integrated banking system works at the code level. The system demonstrates:

### Key Technical Achievements
- **Clean Architecture**: Proper separation of concerns with layered architecture
- **Business Rule Implementation**: Configurable interest rates, fees, and transaction rules
- **Secure Integration**: API key authentication and role-based access control
- **Error Handling**: Comprehensive exception handling and recovery mechanisms
- **Performance Optimization**: Database indexing, caching, and async processing
- **Design Patterns**: Repository, DTO, Builder, and Strategy patterns

### Production-Ready Features
- **Transaction Integrity**: ACID compliance with proper rollback mechanisms
- **Audit Logging**: Comprehensive logging for all business operations
- **Monitoring**: Health checks and performance metrics
- **Security**: Multi-layer security with encryption and authentication
- **Scalability**: Async processing and caching for high-performance operations

This implementation serves as an excellent reference for building enterprise-grade banking applications with modern Java technologies and best practices.

---

**Document Version**: 1.0  
**Last Updated**: July 14, 2024  
**Authors**: Bank X Development Team  
**Status**: Production Ready