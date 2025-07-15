# Bank X Application

A comprehensive banking application built with Spring Boot that provides full banking functionality including customer management, account operations, transaction processing, and external bank integration.

## 🏦 Business Requirements Fulfilled

### Core Banking Features
- ✅ **Customer Onboarding**: New customers automatically receive Current and Savings accounts
- ✅ **Joining Bonus**: R500 credited to Savings account upon registration
- ✅ **Interest Calculation**: 0.5% interest on all Savings account credits
- ✅ **Transaction Fees**: 0.05% fee on all account debits
- ✅ **Payment Restrictions**: Only Current accounts can make payments to other accounts
- ✅ **Account Transfers**: Money transfers between customer's own accounts
- ✅ **Transaction Tracking**: Complete audit trail of all transactions
- ✅ **Customer Notifications**: Real-time notifications for all transaction events

### Bank Z Integration
- ✅ **Single Transaction Processing**: Real-time transaction processing for Bank Z
- ✅ **Batch Processing**: Handle multiple transactions in a single request
- ✅ **End-of-Day Reconciliation**: Offline reconciliation process for Bank Z transactions

## 🚀 Quick Start

### Prerequisites
- Java 17+
- PostgreSQL 12+
- Maven 3.6+

### Database Setup
```bash
# Using Docker (recommended)
docker run --name bankx-postgres \
  -e POSTGRES_DB=bankx_db \
  -e POSTGRES_USER=bankx_user \
  -e POSTGRES_PASSWORD=bankx_password \
  -p 5433:5432 \
  -d postgres:15-alpine
```

### Running the Application
```bash
# Clone and navigate to project
cd bank-x-app

# Build the application
mvn clean compile

# Run the application
mvn spring-boot:run
```

### Access the Application
- **Web Interface**: http://41.76.107.45:9090
- **Admin Login**: username: `admin`, password: `admin123` (Real database authentication)
- **Customer Registration**: http://41.76.107.45:9090/register (Creates real database user)
- **Customer Login**: Use credentials created during registration
- **Health Check**: http://41.76.107.45:9090/actuator/health

### Authentication System
- **Real Database**: PostgreSQL with encrypted passwords (BCrypt)
- **Admin User**: Automatically created on startup with database persistence
- **Customer Users**: Created through registration with full database integration
- **Security**: Spring Security with role-based access control (ADMIN/CUSTOMER)
- **Session Management**: Secure session handling with CSRF protection

## 🌐 Web Interface

### Customer Portal Features
- **Dashboard**: Account overview with balances and recent transactions
- **Account Management**: View account details and transaction history
- **Money Transfers**: Transfer funds between own accounts
- **Transaction History**: Complete transaction audit trail

### Admin Portal Features
- **System Dashboard**: Overview of customers, accounts, and transactions
- **Customer Management**: View and manage all customers
- **Account Monitoring**: Monitor all accounts and balances
- **Transaction Oversight**: View all system transactions

## 📡 API Endpoints

### Customer Management
```http
POST /api/customers              # Register new customer
GET  /api/customers              # List all customers
GET  /api/customers/{id}         # Get customer details
GET  /api/customers/{id}/accounts # Get customer accounts
```

### Account Operations
```http
GET  /api/accounts/{accountNumber}         # Get account details
GET  /api/accounts/{accountNumber}/balance # Get account balance
POST /api/accounts/{accountNumber}/credit  # Credit account
POST /api/accounts/{accountNumber}/debit   # Debit account
```

### Transaction Processing
```http
POST /api/transactions/transfer  # Transfer between accounts
POST /api/transactions/payment   # Make payment to another account
GET  /api/transactions/{id}      # Get transaction details
```

### Bank Z Integration
```http
POST /api/bankz/transaction      # Process single transaction
POST /api/bankz/batch           # Process batch transactions
POST /api/bankz/reconciliation  # End-of-day reconciliation
```

## 🏗️ Architecture

### Technology Stack
- **Backend**: Spring Boot 3.2.0, Java 17
- **Database**: PostgreSQL with Hibernate/JPA
- **Security**: Spring Security with BCrypt encryption
- **Frontend**: Thymeleaf templates with responsive CSS
- **Testing**: JUnit 5, Mockito, Spring Boot Test
- **Build Tool**: Maven

### Key Components
- **Entities**: Customer, Account, Transaction, User, Notification
- **Services**: Business logic with transaction management
- **Controllers**: REST API and web controllers
- **Repositories**: JPA repositories with custom queries
- **Security**: Role-based authentication and authorization

## 💳 Account Types

### Current Account
- Used for daily transactions and payments
- Can make payments to other accounts
- Subject to 0.05% transaction fee on debits

### Savings Account
- Earns 0.5% interest on all credits
- Receives R500 joining bonus
- Cannot make payments to external accounts

## 🔒 Security Features

- **Authentication**: Username/password with BCrypt encryption
- **Authorization**: Role-based access control (Admin/Customer)
- **Session Management**: Secure session handling
- **Input Validation**: Comprehensive data validation
- **SQL Injection Protection**: Parameterized queries
- **XSS Protection**: Output encoding and CSRF tokens

## 🧪 Testing

### Running Tests
```bash
# Run all tests
mvn test
jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjj
# Run specific test class
mvn test -Dtest=CustomerServiceTest

# Run with coverage
mvn test jacoco:report
```

### Test Coverage
- Unit tests for all service classes
- Integration tests for controllers
- Repository tests with H2 database
- End-to-end API tests

## 📊 Sample API Usage

### Register New Customer
```bash
curl -X POST http://localhost:9090/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+27123456789"
  }'
```

### Transfer Money Between Accounts
```bash
curl -X POST http://localhost:9090/api/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountNumber": "CUR1234567890",
    "toAccountNumber": "SAV1234567891",
    "amount": 100.00,
    "description": "Monthly savings"
  }'
```

### Bank Z Transaction Processing
```bash
curl -X POST http://localhost:9090/api/bankz/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "CUR1234567890",
    "amount": 50.00,
    "transactionType": "CREDIT",
    "reference": "BANKZ-001"
  }'
```

## 🎨 UI Design

The web interface features an ABSA-inspired design with:
- Professional red and white color scheme
- Responsive layout for mobile and desktop
- Clean, modern interface
- Intuitive navigation
- Real-time updates

## 📝 Configuration

### Application Properties
```properties
# Server Configuration
server.port=9090

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5433/bankx_db
spring.datasource.username=bankx_user
spring.datasource.password=bankx_password

# Banking Configuration
bankx.joining-bonus=500.00
bankx.savings-interest-rate=0.005
bankx.transaction-fee-rate=0.0005
```

## 🚀 Deployment

### Production Deployment
1. Build the application: `mvn clean package`
2. Set up PostgreSQL database
3. Configure environment variables
4. Run: `java -jar target/bank-x-app-1.0.0.jar`

### Docker Deployment
```bash
# Build Docker image
docker build -t bank-x-app .

# Run with Docker Compose
docker-compose up -d
```

## 📞 Support

For questions or issues, please refer to the implementation documentation or create an issue in the repository.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

**Bank X Application** - A complete banking solution that runs on its own with all business requirements fulfilled.ed