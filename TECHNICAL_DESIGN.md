# Bank X Application - Technical Design Document

## 1. Executive Summary

The Bank X Application is a comprehensive banking system designed to handle customer onboarding, account management, transactions, and inter-bank operations. The system provides REST APIs for both internal and external system integration, with real-time transaction processing and notification capabilities.

## 2. System Architecture

### 2.1 High-Level Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   External      │    │   Bank X        │    │   Bank Z        │
│   Systems       │◄──►│   Application   │◄──►│   Integration   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │   PostgreSQL    │
                       │   Database      │
                       └─────────────────┘
```

### 2.2 Technology Stack
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL 15+
- **ORM**: Spring Data JPA / Hibernate
- **API**: REST with JSON
- **Build Tool**: Maven
- **Packaging**: Standalone JAR with embedded Tomcat

### 2.3 Application Layers
1. **Controller Layer**: REST API endpoints
2. **Service Layer**: Business logic implementation
3. **Repository Layer**: Data access abstraction
4. **Entity Layer**: JPA entities and database mapping
5. **DTO Layer**: Data transfer objects for API communication

## 3. Database Design

### 3.1 Entity Relationship Diagram
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│    Customer     │    │    Account      │    │   Transaction   │
├─────────────────┤    ├─────────────────┤    ├────────────���────┤
│ id (PK)         │    │ id (PK)         │    │ id (PK)         │
│ first_name      │◄──►│ customer_id(FK) │◄──►│ account_id (FK) │
│ last_name       │    │ account_number  │    │ transaction_id  │
│ email           │    │ account_type    │    │ amount          │
│ phone_number    │    │ balance         │    │ transaction_type│
│ created_at      │    │ created_at      │    │ description     │
│ updated_at      │    │ updated_at      │    │ created_at      │
└─────────────────┘    └─────────────────┘    │ status          │
                                              │ reference_number│
                                              └─────────────────┘

┌─────────────────┐    ┌─────────────────┐
│  Notification   │    │  BankZTransaction│
├─────────────────┤    ├─────────────────┤
│ id (PK)         │    │ id (PK)         │
│ customer_id(FK) │    │ customer_id(FK) │
│ message         │    │ account_number  │
│ type            │    │ amount          │
│ sent_at         │    │ transaction_type│
│ status          │    │ reference       │
└─────────────────┘    │ processed_at    │
                       │ batch_id        │
                       └─────────────────┘
```

### 3.2 Database Schema

#### 3.2.1 Customer Table
```sql
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 3.2.2 Account Table
```sql
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    account_number VARCHAR(20) UNIQUE NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('CURRENT', 'SAVINGS')),
    balance DECIMAL(15,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 3.2.3 Transaction Table
```sql
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    transaction_id VARCHAR(50) UNIQUE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('CREDIT', 'DEBIT', 'TRANSFER')),
    description TEXT,
    reference_number VARCHAR(100),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 3.2.4 Notification Table
```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'SENT'
);
```

#### 3.2.5 Bank Z Transaction Table
```sql
CREATE TABLE bankz_transactions (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    account_number VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    reference VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    batch_id VARCHAR(50)
);
```

## 4. API Design

### 4.1 Customer Management APIs
- `POST /api/customers` - Onboard new customer
- `GET /api/customers/{id}` - Get customer details
- `GET /api/customers/{id}/accounts` - Get customer accounts

### 4.2 Account Management APIs
- `GET /api/accounts/{accountNumber}` - Get account details
- `GET /api/accounts/{accountNumber}/balance` - Get account balance
- `POST /api/accounts/transfer` - Transfer between accounts

### 4.3 Transaction APIs
- `POST /api/transactions/payment` - Make payment to other accounts
- `GET /api/transactions/account/{accountNumber}` - Get account transactions
- `GET /api/transactions/{transactionId}` - Get specific transaction

### 4.4 Bank Z Integration APIs
- `POST /api/bankz/transaction` - Process single Bank Z transaction
- `POST /api/bankz/transactions/batch` - Process batch transactions
- `POST /api/bankz/reconciliation` - End-of-day reconciliation

### 4.5 Notification APIs
- `GET /api/notifications/customer/{customerId}` - Get customer notifications

## 5. Business Rules Implementation

### 5.1 Customer Onboarding
1. Create customer record
2. Generate unique account numbers for Current and Savings accounts
3. Credit R500.00 joining bonus to Savings account
4. Send welcome notification

### 5.2 Transaction Processing
1. **Interest Calculation**: 0.5% interest on Savings account credits
2. **Transaction Fee**: 0.05% fee on all debits
3. **Payment Restrictions**: Only Current accounts can make payments to other accounts
4. **Balance Validation**: Ensure sufficient funds before processing debits

### 5.3 Notification System
1. Send notification for every transaction
2. Include transaction details and updated balance
3. Store notification history

## 6. Security Considerations

### 6.1 Authentication & Authorization
- **Real Database Authentication**: PostgreSQL with encrypted passwords
- **Password Encryption**: BCrypt with salt for secure password storage
- **Role-based Access Control**: ADMIN and CUSTOMER roles
- **Spring Security**: Enterprise-level authentication framework
- **Session Management**: Secure session handling with CSRF protection

### 6.2 User Management
```sql
-- Users table for authentication
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,        -- BCrypt encrypted
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255),
    role VARCHAR(255) NOT NULL CHECK (role IN ('ADMIN','CUSTOMER')),
    is_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

### 6.3 Data Validation
- Input validation for all API endpoints
- Account number format validation
- Amount validation (positive values, decimal precision)
- Username and email uniqueness validation

### 6.4 Transaction Integrity
- Database transactions for multi-step operations
- Optimistic locking for concurrent balance updates
- Idempotency for Bank Z transactions
- SQL injection prevention through JPA/Hibernate

### 6.5 Web Security
- Form-based authentication with login/logout
- CSRF protection for web forms
- XSS protection through output encoding
- Secure session cookies

## 7. Performance Considerations

### 7.1 Database Optimization
- Indexes on frequently queried columns
- Connection pooling
- Query optimization

### 7.2 Caching Strategy
- Account balance caching for read-heavy operations
- Customer data caching

## 8. Deployment Architecture

### 8.1 Standalone Deployment
- Single JAR file with embedded Tomcat
- External PostgreSQL database
- Configuration through application.properties

### 8.2 Environment Configuration
- Development: Local PostgreSQL
- Production: External PostgreSQL with connection pooling

## 9. Monitoring and Logging

### 9.1 Application Logging
- Transaction audit logs
- Error logging with stack traces
- Performance metrics logging

### 9.2 Health Checks
- Database connectivity checks
- Application health endpoints

## 10. Testing Strategy

### 10.1 Unit Testing
- Service layer business logic testing
- Repository layer data access testing

### 10.2 Integration Testing
- API endpoint testing
- Database integration testing
- Bank Z integration testing

## 11. Future Enhancements

### 11.1 Scalability
- Microservices architecture
- Event-driven architecture with message queues
- Horizontal scaling capabilities

### 11.2 Additional Features
- Real-time notifications via WebSocket
- Advanced reporting and analytics
- Mobile API support