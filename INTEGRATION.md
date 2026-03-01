# API Gateway Integration Guide

**Smart Bank Elite System - Service Integration & Communication**

## 📑 Table of Contents

1. [Overview](#overview)
2. [Integration Architecture](#integration-architecture)
3. [AUTH-SERVER Integration](#auth-server-integration)
4. [ACCOUNT-SERVICE Integration](#account-service-integration)
5. [TRANSACTION-SERVICE Integration](#transaction-service-integration)
6. [EUREKA Integration](#eureka-integration)
7. [Inter-Service Communication](#inter-service-communication)
8. [Request/Response Flow](#requestresponse-flow)
9. [Error Handling](#error-handling)
10. [Configuration & Setup](#configuration--setup)
11. [Testing Integration](#testing-integration)
12. [Troubleshooting](#troubleshooting)

---

## 🎯 Overview

The API Gateway acts as the central hub for all microservices in the Smart Bank Elite System. This document provides detailed integration specifications for:

- **AUTH-SERVER**: Authentication & JWT token management
- **ACCOUNT-SERVICE**: Account operations & management
- **TRANSACTION-SERVICE**: Financial transactions & transfers
- **EUREKA SERVER**: Service discovery & registration

---

## 🏗️ Integration Architecture

### High-Level Integration Diagram

```
┌───────────────────────────────────────────────────────────────┐
│                    CLIENT APPLICATIONS                        │
│          (Web, Mobile, Desktop, External APIs)               │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP/HTTPS
                           ▼
        ┌──────────────────────────────────────┐
        │      API GATEWAY (Port 8080)         │
        │                                      │
        │  - Request Routing                   │
        │  - JWT Validation                    │
        │  - Load Balancing                    │
        │  - Request/Response Logging          │
        │  - Error Handling                    │
        └──────────────────────────────────────┘
                           │
            ┌──────────────┼──────────────┬─────────────┐
            │              │              │             │
            ▼              ▼              ▼             ▼
      ┌──────────┐  ┌───────────────┐ ┌──────────┐ ┌────────────┐
      │   AUTH   │  │   ACCOUNT     │ │TRANSACTION│ │  EUREKA    │
      │ SERVER   │  │   SERVICE     │ │ SERVICE  │ │  SERVER    │
      │ :8085    │  │  (Eureka)     │ │(Eureka)  │ │  :8761     │
      └──────────┘  └───────────────┘ └──────────┘ └────────────┘
            │              │              │
            └──────────────┴──────────────┘
                    │ Service-to-Service
                    │ Communication
                    ▼
            (Optional Direct Calls)
```

### Integration Points

```
┌─────────────────────────────────────────────────────────────┐
│                   API GATEWAY                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  1. AUTH-SERVER Integration                         │  │
│  │     ├─ Direct HTTP connection (localhost:8085)      │  │
│  │     ├─ JWT token generation & validation            │  │
│  │     ├─ User authentication                          │  │
│  │     └─ Password management                          │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  2. ACCOUNT-SERVICE Integration                     │  │
│  │     ├─ Eureka service discovery                     │  │
│  │     ├─ Load-balanced routing (lb://ACCOUNT-SERVICE) │  │
│  │     ├─ Account operations                           │  │
│  │     └─ Balance inquiry                              │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  3. TRANSACTION-SERVICE Integration                 │  │
│  │     ├─ Eureka service discovery                     │  │
│  │     ├─ Load-balanced routing                        │  │
│  │     ├─ Transaction processing                       │  │
│  │     └─ Transaction history                          │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  4. EUREKA Integration                              │  │
│  │     ├─ Service registration                         │  │
│  │     ├─ Health checks                                │  │
│  │     ├─ Dynamic discovery                            │  │
│  │     └─ Load balancer integration                    │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔐 AUTH-SERVER Integration

### Overview

The AUTH-SERVER is the security backbone of the Smart Bank Elite System. The API Gateway integrates with AUTH-SERVER for:
- User authentication and login
- JWT token generation
- Token validation
- User registration
- Password management

### Configuration

```properties
# application.properties
# Route to AUTH-SERVER
spring.cloud.gateway.routes[0].id=AUTH-SERVER
spring.cloud.gateway.routes[0].uri=http://localhost:8085
spring.cloud.gateway.routes[0].predicates[0]=Path=/auth-server/**
spring.cloud.gateway.routes[0].filters[0]=StripPrefix=1

# JWT Configuration
security.jwt.secret-key=3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b
security.jwt.expiration-time=86400000  # 24 hours in milliseconds
```

### Service Details

| Property | Value |
|----------|-------|
| **Service Name** | AUTH-SERVER |
| **Port** | 8085 (Direct connection) |
| **Discovery** | Direct HTTP (not via Eureka) |
| **Base URL** | `http://localhost:8085` |
| **Gateway Path** | `/auth-server/**` |
| **Protocol** | HTTP/HTTPS |
| **Authentication** | JWT tokens |

### API Endpoints

#### 1. **User Login (Unsecured)**

```
Endpoint: POST /auth-server/unsecure/login
Gateway Path: /auth-server/unsecure/login
Target Service Path: /unsecure/login

Description: Authenticate user and receive JWT token

Request Headers:
  Content-Type: application/json

Request Body:
{
  "username": "john_doe",
  "password": "secure_password_123"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwiZXhwIjoxNjQ1NjUxMjAwLCJpYXQiOjE2NDU1NjQ4MDB9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
  "username": "john_doe",
  "userId": "user123",
  "expiresIn": 86400
}

Error Response (401 Unauthorized):
{
  "status": 401,
  "error": "Invalid credentials",
  "message": "Username or password is incorrect",
  "timestamp": "2026-03-01T10:30:00.000Z"
}

cURL Example:
curl -X POST http://localhost:8080/auth-server/unsecure/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "secure_password_123"
  }'
```

#### 2. **User Registration (Unsecured)**

```
Endpoint: POST /auth-server/unsecure/register
Gateway Path: /auth-server/unsecure/register
Target Service Path: /unsecure/register

Description: Register a new user account

Request Headers:
  Content-Type: application/json

Request Body:
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "secure_password_123",
  "firstName": "John",
  "lastName": "Doe"
}

Response (201 Created):
{
  "userId": "user123",
  "username": "john_doe",
  "email": "john@example.com",
  "message": "User registered successfully",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

Error Response (400 Bad Request):
{
  "status": 400,
  "error": "Registration failed",
  "message": "Username already exists",
  "timestamp": "2026-03-01T10:30:00.000Z"
}

cURL Example:
curl -X POST http://localhost:8080/auth-server/unsecure/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "secure_password_123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

#### 3. **Validate Token (Secured)**

```
Endpoint: POST /auth-server/validate-token
Gateway Path: /auth-server/validate-token
Target Service Path: /validate-token

Description: Validate JWT token (used by services)

Request Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
  Content-Type: application/json

Response (200 OK):
{
  "valid": true,
  "userId": "user123",
  "username": "john_doe",
  "expiresAt": 1645651200,
  "roles": ["USER", "ACCOUNT_HOLDER"]
}

Error Response (401 Unauthorized):
{
  "valid": false,
  "error": "Token expired",
  "message": "JWT token has expired"
}

cURL Example:
curl -X POST http://localhost:8080/auth-server/validate-token \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

#### 4. **Refresh Token (Secured)**

```
Endpoint: POST /auth-server/refresh-token
Gateway Path: /auth-server/refresh-token
Target Service Path: /refresh-token

Description: Get a new JWT token using existing token

Request Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIi...",
  "expiresIn": 86400,
  "tokenType": "Bearer"
}

cURL Example:
curl -X POST http://localhost:8080/auth-server/refresh-token \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Authentication Flow

```
1. USER LOGIN
   Client → POST /auth-server/unsecure/login
           (username, password)
            │
            ▼
   API Gateway:
   - LoggingFilter: Log login attempt
   - JwtAuthenticationFilter: Skip (unsecure endpoint)
   - Route to AUTH-SERVER:8085/unsecure/login
            │
            ▼
   AUTH-SERVER:
   - Verify credentials against user database
   - Generate JWT token
   - Return token to gateway
            │
            ▼
   API Gateway:
   - Return response to client
            │
            ▼
   Client: Store JWT token in localStorage/sessionStorage

2. SUBSEQUENT REQUESTS
   Client → GET /account/details
           (with Authorization: Bearer <token>)
            │
            ▼
   API Gateway:
   - LoggingFilter: Log request
   - JwtAuthenticationFilter: 
     * Extract token from Authorization header
     * Validate token format (Bearer ...)
     * Check token signature (using JWT secret)
     * Check expiration time
     * Token valid? → Continue
     * Token invalid? → Return 401 Unauthorized
   - AuthHeaderForwardFilter: 
     * Keep Authorization header for downstream service
   - Route to ACCOUNT-SERVICE
            │
            ▼
   ACCOUNT-SERVICE:
   - Process request
   - Return response
            │
            ▼
   API Gateway:
   - Return response to client
```

### JWT Token Structure

```
Header:
{
  "alg": "HS256",           // Algorithm
  "typ": "JWT"              // Token type
}

Payload:
{
  "sub": "user123",         // Subject (user ID)
  "username": "john_doe",   // Username
  "email": "john@example.com", // Email
  "roles": ["USER"],        // User roles
  "iat": 1645564800,        // Issued at (timestamp)
  "exp": 1645651200         // Expiration (timestamp)
}

Signature:
HMACSHA256(
  base64(header) + "." + base64(payload),
  "3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b"
)
```

### Security Considerations

- ✅ Credentials transmitted over HTTPS only (configure in production)
- ✅ JWT tokens have expiration time (default: 24 hours)
- ✅ Secret key stored securely (use environment variables in production)
- ✅ Token validation on every secured request
- ✅ Password hashing (bcrypt) on AUTH-SERVER
- ⚠️ Update JWT secret key periodically
- ⚠️ Implement refresh token mechanism
- ⚠️ Add rate limiting on login endpoint

---

## 💰 ACCOUNT-SERVICE Integration

### Overview

The ACCOUNT-SERVICE manages all banking account operations. It integrates with the API Gateway through Eureka for:
- Account creation and management
- Account details retrieval
- Balance inquiries
- Account transactions (linked with TRANSACTION-SERVICE)

### Configuration

```properties
# application.properties
# Route to ACCOUNT-SERVICE via Eureka
spring.cloud.gateway.routes[1].id=ACCOUNT-SERVICE
spring.cloud.gateway.routes[1].uri=lb://ACCOUNT-SERVICE
spring.cloud.gateway.routes[1].predicates[0]=Path=/account/**
spring.cloud.gateway.routes[1].filters[0]=StripPrefix=1

# Eureka Discovery Configuration
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
```

### Service Details

| Property | Value |
|----------|-------|
| **Service Name** | ACCOUNT-SERVICE |
| **Port** | Dynamic (via Eureka) |
| **Discovery** | Eureka Service Registry |
| **Load Balancing** | Ribbon (Round-robin) |
| **Gateway Route** | `lb://ACCOUNT-SERVICE` |
| **Base Path** | `/account/**` |
| **Authentication** | JWT tokens (required) |

### API Endpoints

#### 1. **Get Account Details**

```
Endpoint: GET /account/get-account/{accountId}
Gateway Path: /account/get-account/ACC123
Target Service Path: /get-account/ACC123 (after StripPrefix)

Description: Retrieve account information

Request Headers:
  Authorization: Bearer <JWT_TOKEN>
  Accept: application/json

Path Parameters:
  accountId: ACC123 (unique account identifier)

Response (200 OK):
{
  "accountId": "ACC123",
  "accountHolder": "John Doe",
  "accountType": "SAVINGS",
  "balance": 50000.00,
  "currency": "USD",
  "accountStatus": "ACTIVE",
  "createdAt": "2025-01-15T10:30:00.000Z",
  "lastModified": "2026-02-28T15:45:00.000Z",
  "accountNumber": "1234567890",
  "ifscCode": "BANK0001"
}

Error Response (404 Not Found):
{
  "status": 404,
  "error": "Account not found",
  "message": "Account ACC123 does not exist",
  "timestamp": "2026-03-01T10:30:00.000Z"
}

Error Response (401 Unauthorized):
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Valid JWT token required"
}

cURL Example:
curl -X GET http://localhost:8080/account/get-account/ACC123 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

#### 2. **Create New Account**

```
Endpoint: POST /account/create-account
Gateway Path: /account/create-account
Target Service Path: /create-account

Description: Create a new account for user

Request Headers:
  Authorization: Bearer <JWT_TOKEN>
  Content-Type: application/json

Request Body:
{
  "userId": "user123",
  "accountType": "SAVINGS",
  "initialDeposit": 1000.00,
  "currency": "USD",
  "accountName": "My Savings Account"
}

Response (201 Created):
{
  "accountId": "ACC124",
  "userId": "user123",
  "accountType": "SAVINGS",
  "balance": 1000.00,
  "currency": "USD",
  "accountStatus": "ACTIVE",
  "accountNumber": "1234567891",
  "message": "Account created successfully"
}

cURL Example:
curl -X POST http://localhost:8080/account/create-account \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "accountType": "SAVINGS",
    "initialDeposit": 1000.00,
    "currency": "USD"
  }'
```

#### 3. **Update Account Information**

```
Endpoint: PUT /account/update-account/{accountId}
Gateway Path: /account/update-account/ACC123
Target Service Path: /update-account/ACC123

Description: Update account details

Request Headers:
  Authorization: Bearer <JWT_TOKEN>
  Content-Type: application/json

Request Body:
{
  "accountName": "Updated Account Name",
  "accountStatus": "ACTIVE"
}

Response (200 OK):
{
  "accountId": "ACC123",
  "accountName": "Updated Account Name",
  "message": "Account updated successfully"
}

cURL Example:
curl -X PUT http://localhost:8080/account/update-account/ACC123 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "accountName": "Updated Account Name"
  }'
```

#### 4. **List All Accounts**

```
Endpoint: GET /account/list-accounts
Gateway Path: /account/list-accounts
Target Service Path: /list-accounts

Description: Get all accounts for logged-in user

Request Headers:
  Authorization: Bearer <JWT_TOKEN>

Query Parameters:
  page: 0 (optional, default: 0)
  pageSize: 10 (optional, default: 10)

Response (200 OK):
{
  "accounts": [
    {
      "accountId": "ACC123",
      "accountType": "SAVINGS",
      "balance": 50000.00,
      "accountStatus": "ACTIVE"
    },
    {
      "accountId": "ACC124",
      "accountType": "CHECKING",
      "balance": 10000.00,
      "accountStatus": "ACTIVE"
    }
  ],
  "totalCount": 2,
  "pageNumber": 0,
  "pageSize": 10
}

cURL Example:
curl -X GET "http://localhost:8080/account/list-accounts?page=0&pageSize=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Eureka Service Discovery Flow

```
1. REQUEST ARRIVES AT GATEWAY
   GET /account/get-account/ACC123
   (with Authorization header)
                │
                ▼
   API Gateway:
   - Match route: /account/** → ACCOUNT-SERVICE
   - Service name: ACCOUNT-SERVICE
                │
                ▼
   2. EUREKA LOOKUP
   - Query Eureka: "Where is ACCOUNT-SERVICE?"
   - Eureka response:
     {
       "application": "ACCOUNT-SERVICE",
       "instances": [
         {
           "ipAddr": "192.168.1.100",
           "port": 8081,
           "status": "UP",
           "lastHeartbeat": "2026-03-01T10:29:50.000Z"
         },
         {
           "ipAddr": "192.168.1.101",
           "port": 8082,
           "status": "UP",
           "lastHeartbeat": "2026-03-01T10:29:55.000Z"
         }
       ]
     }
                │
                ▼
   3. LOAD BALANCING
   - Available instances: 2
   - Load balancing strategy: Round-robin
   - Select instance: 192.168.1.100:8081
                │
                ▼
   4. REQUEST FORWARDING
   - StripPrefix=/account
   - Path: /get-account/ACC123
   - Forward to: http://192.168.1.100:8081/get-account/ACC123
                │
                ▼
   5. SERVICE PROCESSING
   - ACCOUNT-SERVICE receives request
   - Validates JWT token
   - Retrieves account details
   - Returns response
                │
                ▼
   6. RESPONSE TO CLIENT
   - Gateway returns response to client
```

### Eureka Registration (ACCOUNT-SERVICE Configuration)

The ACCOUNT-SERVICE must be configured to register with Eureka:

```properties
# In ACCOUNT-SERVICE application.properties
spring.application.name=ACCOUNT-SERVICE
server.port=8081

# Eureka Configuration
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true

# Health Check Configuration
eureka.client.healthcheck.enabled=true
eureka.instance.health-check-url-path=/actuator/health
eureka.instance.instance-id=${spring.application.name}:${server.port}
```

---

## 💳 TRANSACTION-SERVICE Integration

### Overview

The TRANSACTION-SERVICE handles all financial transactions and transfers. It integrates with:
- **API Gateway**: For client requests
- **ACCOUNT-SERVICE**: For balance updates
- **EUREKA**: For service discovery

### Configuration

```properties
# application.properties
# Route to TRANSACTION-SERVICE via Eureka
spring.cloud.gateway.routes[2].id=TRANSACTION-SERVICE
spring.cloud.gateway.routes[2].uri=lb://TRANSACTION-SERVICE
spring.cloud.gateway.routes[2].predicates[0]=Path=/transaction/**
spring.cloud.gateway.routes[2].filters[0]=StripPrefix=1

# Eureka Configuration
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
```

### Service Details

| Property | Value |
|----------|-------|
| **Service Name** | TRANSACTION-SERVICE |
| **Port** | Dynamic (via Eureka) |
| **Discovery** | Eureka Service Registry |
| **Load Balancing** | Ribbon (Round-robin) |
| **Gateway Route** | `lb://TRANSACTION-SERVICE` |
| **Base Path** | `/transaction/**` |
| **Authentication** | JWT tokens (required) |

### API Endpoints

#### 1. **Create Transfer/Transaction**

```
Endpoint: POST /transaction/transfer
Gateway Path: /transaction/transfer
Target Service Path: /transfer

Description: Create a money transfer between accounts

Request Headers:
  Authorization: Bearer <JWT_TOKEN>
  Content-Type: application/json

Request Body:
{
  "fromAccountId": "ACC123",
  "toAccountId": "ACC124",
  "amount": 1000.00,
  "description": "Payment for services",
  "transactionType": "TRANSFER"
}

Response (201 Created):
{
  "transactionId": "TXN001",
  "fromAccountId": "ACC123",
  "toAccountId": "ACC124",
  "amount": 1000.00,
  "status": "PENDING",
  "createdAt": "2026-03-01T10:30:00.000Z",
  "estimatedCompletionTime": "2026-03-01T10:31:00.000Z",
  "message": "Transaction initiated successfully"
}

Error Response (400 Bad Request):
{
  "status": 400,
  "error": "Insufficient balance",
  "message": "Account ACC123 does not have sufficient balance for this transfer"
}

cURL Example:
curl -X POST http://localhost:8080/transaction/transfer \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": "ACC123",
    "toAccountId": "ACC124",
    "amount": 1000.00,
    "description": "Payment for services"
  }'
```

#### 2. **Get Transaction History**

```
Endpoint: GET /transaction/history/{accountId}
Gateway Path: /transaction/history/ACC123
Target Service Path: /history/ACC123

Description: Retrieve transaction history for an account

Request Headers:
  Authorization: Bearer <JWT_TOKEN>

Query Parameters:
  page: 0 (optional)
  pageSize: 20 (optional)
  fromDate: 2026-01-01 (optional, ISO format)
  toDate: 2026-03-01 (optional, ISO format)

Response (200 OK):
{
  "accountId": "ACC123",
  "transactions": [
    {
      "transactionId": "TXN001",
      "type": "TRANSFER",
      "fromAccountId": "ACC123",
      "toAccountId": "ACC124",
      "amount": 1000.00,
      "status": "COMPLETED",
      "timestamp": "2026-03-01T10:30:00.000Z"
    },
    {
      "transactionId": "TXN002",
      "type": "DEPOSIT",
      "accountId": "ACC123",
      "amount": 5000.00,
      "status": "COMPLETED",
      "timestamp": "2026-02-28T15:00:00.000Z"
    }
  ],
  "totalCount": 2,
  "pageNumber": 0,
  "pageSize": 20
}

cURL Example:
curl -X GET "http://localhost:8080/transaction/history/ACC123?page=0&pageSize=20" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 3. **Get Transaction Details**

```
Endpoint: GET /transaction/details/{transactionId}
Gateway Path: /transaction/details/TXN001
Target Service Path: /details/TXN001

Description: Get detailed information about a specific transaction

Request Headers:
  Authorization: Bearer <JWT_TOKEN>

Response (200 OK):
{
  "transactionId": "TXN001",
  "fromAccountId": "ACC123",
  "toAccountId": "ACC124",
  "amount": 1000.00,
  "currency": "USD",
  "type": "TRANSFER",
  "status": "COMPLETED",
  "initiatedAt": "2026-03-01T10:30:00.000Z",
  "completedAt": "2026-03-01T10:30:15.000Z",
  "description": "Payment for services",
  "referenceNumber": "REF123456",
  "charges": 2.50,
  "netAmount": 997.50
}

cURL Example:
curl -X GET http://localhost:8080/transaction/details/TXN001 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 4. **Approve Transaction (Admin)**

```
Endpoint: PUT /transaction/approve/{transactionId}
Gateway Path: /transaction/approve/TXN001
Target Service Path: /approve/TXN001

Description: Approve a pending transaction (admin only)

Request Headers:
  Authorization: Bearer <JWT_TOKEN> (admin role required)
  Content-Type: application/json

Request Body:
{
  "approvalReason": "Verified by admin",
  "approvedBy": "admin_user"
}

Response (200 OK):
{
  "transactionId": "TXN001",
  "status": "APPROVED",
  "approvedAt": "2026-03-01T10:35:00.000Z",
  "message": "Transaction approved successfully"
}

cURL Example:
curl -X PUT http://localhost:8080/transaction/approve/TXN001 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "approvalReason": "Verified by admin"
  }'
```

### Service-to-Service Communication

TRANSACTION-SERVICE may call ACCOUNT-SERVICE internally:

```
1. CLIENT INITIATES TRANSFER
   Client → POST /transaction/transfer
           (amount: 1000, from: ACC123, to: ACC124)
            │
            ▼
   API GATEWAY:
   - Validate JWT token
   - Route to TRANSACTION-SERVICE
            │
            ▼
   2. TRANSACTION-SERVICE RECEIVES REQUEST
   - Extract transaction details
   - Prepare to execute transfer
            │
            ▼
   3. VERIFY SOURCE ACCOUNT
   - Call ACCOUNT-SERVICE:
     GET /get-account/ACC123
     (using direct service-to-service call)
            │
            ▼
   ACCOUNT-SERVICE:
   - Return account details with balance
            │
            ▼
   4. VALIDATE BALANCE
   - TRANSACTION-SERVICE checks balance
   - Balance: 5000.00, Request: 1000.00
   - Status: SUFFICIENT BALANCE
            │
            ▼
   5. DEBIT SOURCE ACCOUNT
   - Call ACCOUNT-SERVICE:
     PUT /update-account/ACC123
     (deduct 1000.00)
            │
            ▼
   6. CREDIT DESTINATION ACCOUNT
   - Call ACCOUNT-SERVICE:
     PUT /update-account/ACC124
     (add 1000.00)
            │
            ▼
   7. RECORD TRANSACTION
   - Create transaction record
   - Set status: COMPLETED
            │
            ▼
   8. RESPONSE TO GATEWAY
   - Return transaction details
            │
            ▼
   API GATEWAY:
   - Return response to client
            │
            ▼
   CLIENT:
   - Receives transaction confirmation
```

---

## 🔍 EUREKA Integration

### Overview

EUREKA is the Service Discovery system that enables:
- Service registration
- Service discovery
- Health monitoring
- Load balancing

### Configuration

```properties
# API Gateway application.properties
# Eureka Server Location
eureka.client.service-url.defaultZone=http://localhost:8761/eureka

# Register gateway with Eureka (optional)
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true

# Service Discovery Configuration
spring.cloud.gateway.discovery.locator.enabled=true
spring.cloud.gateway.discovery.locator.lower-case-service-id=true
```

### Eureka Server Details

| Property | Value |
|----------|-------|
| **Service Name** | Eureka Server |
| **Port** | 8761 |
| **URL** | `http://localhost:8761` |
| **Dashboard** | `http://localhost:8761` |
| **Registry API** | `http://localhost:8761/eureka/apps` |
| **Health Check** | `http://localhost:8761/eureka/apps/APP-NAME` |

### Service Registration Process

```
SERVICE STARTUP SEQUENCE:

1. SERVICE STARTS
   ├─ Load configuration from application.properties
   ├─ Read eureka.client.service-url.defaultZone
   ├─ Create Eureka Client
   │
   ▼
2. REGISTER WITH EUREKA
   └─ POST http://localhost:8761/eureka/apps/SERVICE-NAME
      {
        "instance": {
          "instanceId": "SERVICE-NAME:8081",
          "app": "SERVICE-NAME",
          "ipAddr": "192.168.1.100",
          "port": 8081,
          "statusPageUrl": "http://192.168.1.100:8081/actuator",
          "healthCheckUrl": "http://192.168.1.100:8081/actuator/health",
          "homePageUrl": "http://192.168.1.100:8081/",
          "metadata": {
            "version": "1.0"
          },
          "status": "UP"
        }
      }
        │
        ▼
   EUREKA RESPONSE:
   - Confirms registration
   - Service appears in registry
   - Service is discoverable
        │
        ▼
3. HEARTBEAT MECHANISM
   - Every 30 seconds: Send heartbeat to Eureka
   - Eureka updates last-heartbeat timestamp
   - If no heartbeat for 90 seconds: Mark as DOWN
        │
        ▼
4. API GATEWAY DISCOVERY
   - Gateway fetches registry periodically (30s)
   - Builds cache of available services
   - Knows all instances of SERVICE-NAME
        │
        ▼
5. REQUEST ROUTING
   - Request for SERVICE-NAME arrives
   - Gateway queries local cache
   - Selects available instance (load balance)
   - Forwards request to instance
```

### Eureka Dashboard

Access at: `http://localhost:8761`

**Dashboard Shows:**

```
┌─────────────────────────────────────────────────┐
│         EUREKA SERVICE REGISTRY                 │
├─────────────────────────────────────────────────┤
│                                                 │
│ Applications:                                   │
│                                                 │
│ API-GATEWAY                                     │
│ ├─ Status: UP                                   │
│ ├─ Instance: API-GATEWAY:8080                  │
│ ├─ IP: 192.168.1.99                            │
│ ├─ Port: 8080                                  │
│ └─ Last Heartbeat: Just now                    │
│                                                 │
│ AUTH-SERVER (1 instance)                        │
│ ├─ Status: UP                                   │
│ ├─ Instance: AUTH-SERVER:8085                  │
│ ├─ IP: 192.168.1.98                            │
│ ├─ Port: 8085                                  │
│ └─ Last Heartbeat: Just now                    │
│                                                 │
│ ACCOUNT-SERVICE (2 instances)                   │
│ ├─ Instance 1: ACCOUNT-SERVICE:8081            │
│ │  ├─ Status: UP                               │
│ │  ├─ IP: 192.168.1.100                        │
│ │  └─ Last Heartbeat: Just now                 │
│ └─ Instance 2: ACCOUNT-SERVICE:8082            │
│    ├─ Status: UP                               │
│    ├─ IP: 192.168.1.101                        │
│    └─ Last Heartbeat: 2 seconds ago             │
│                                                 │
│ TRANSACTION-SERVICE (2 instances)               │
│ ├─ Instance 1: TRANSACTION-SERVICE:8083        │
│ │  ├─ Status: UP                               │
│ │  └─ Last Heartbeat: 5 seconds ago             │
│ └─ Instance 2: TRANSACTION-SERVICE:8084        │
│    ├─ Status: DOWN                             │
│    └─ Last Heartbeat: 95 seconds ago            │
│                                                 │
│ Total Registered Instances: 7                  │
│ Total Instances Down: 1                        │
│                                                 │
└─────────────────────────────────────────────────┘
```

### Programmatic Access to Registry

```java
// Get registry information programmatically
// Using DiscoveryClient interface

@Autowired
private DiscoveryClient discoveryClient;

// Get all instances of a service
List<ServiceInstance> instances = 
  discoveryClient.getInstances("ACCOUNT-SERVICE");

// Get specific instance
ServiceInstance instance = instances.get(0);
String instanceId = instance.getInstanceId();
String host = instance.getHost();
int port = instance.getPort();
String url = instance.getUri().toString();

// Get all services
List<String> services = discoveryClient.getServices();
// Output: [API-GATEWAY, AUTH-SERVER, ACCOUNT-SERVICE, TRANSACTION-SERVICE]
```

---

## 🔄 Inter-Service Communication

### Service-to-Service Calls (Without Gateway)

Services may communicate directly without going through the gateway:

```
ACCOUNT-SERVICE              TRANSACTION-SERVICE
        │                            │
        └────────────────────────────┘
          (Direct HTTP call)

Example: TRANSACTION-SERVICE → ACCOUNT-SERVICE

Using RestTemplate or WebClient:

@Service
public class TransactionService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public void transferMoney(String fromAccount, String toAccount, double amount) {
        // Call ACCOUNT-SERVICE directly
        String url = "http://ACCOUNT-SERVICE/get-account/" + fromAccount;
        AccountResponse response = restTemplate.getForObject(url, AccountResponse.class);
        
        // Check balance
        if (response.getBalance() >= amount) {
            // Debit source account
            restTemplate.put(
                "http://ACCOUNT-SERVICE/update-account/" + fromAccount,
                new UpdateRequest(amount, "DEBIT")
            );
            
            // Credit destination account
            restTemplate.put(
                "http://ACCOUNT-SERVICE/update-account/" + toAccount,
                new UpdateRequest(amount, "CREDIT")
            );
        }
    }
}
```

### Communication Patterns

#### **1. Request-Response Pattern**
```
Service A sends request → Service B processes → Returns response
```

#### **2. Asynchronous Pattern (Future Enhancement)**
```
Service A sends message → Message Queue → Service B processes asynchronously
```

#### **3. Event-Driven Pattern (Future Enhancement)**
```
Service A publishes event → Event Bus → Service B listens and reacts
```

---

## 📤 Request/Response Flow

### Complete Request Flow Diagram

```
┌─────────────────────────────────────────────────────┐
│                   CLIENT REQUEST                    │
│  GET /account/get-account/ACC123                   │
│  Authorization: Bearer <JWT>                        │
│  Content-Type: application/json                     │
└──────────────┬──────────────────────────────────────┘
               │
               ▼
    ┌──────────────────────────┐
    │  1. REQUEST RECEIVED     │
    │  by Netty Server         │
    └──────────────┬───────────┘
                   │
                   ▼
    ┌──────────────────────────────────────┐
    │  2. CREATE ServerWebExchange          │
    │  - Request: GET /account/...          │
    │  - Headers: Authorization, ...        │
    │  - Path: /account/get-account/ACC123  │
    └──────────────┬───────────────────────┘
                   │
                   ▼
    ┌──────────────────────────────────────┐
    │  3. FILTER CHAIN EXECUTION           │
    │                                      │
    │  LoggingFilter:                      │
    │  └─ println("Gateway received...")   │
    │                                      │
    │  JwtAuthenticationFilter:            │
    │  ├─ Extract token from header        │
    │  ├─ Validate format (Bearer ...)     │
    │  ├─ Verify signature                 │
    │  ├─ Check expiration                 │
    │  └─ Token valid? YES → Continue      │
    │                                      │
    │  AuthHeaderForwardFilter:            │
    │  └─ Keep Authorization header        │
    │                                      │
    │  GatewayFilterChain:                 │
    │  ├─ Match route: /account/**         │
    │  ├─ Service: ACCOUNT-SERVICE         │
    │  ├─ Query Eureka for instances       │
    │  ├─ Select: 192.168.1.100:8081       │
    │  ├─ Apply StripPrefix=/account       │
    │  └─ New path: /get-account/ACC123    │
    └──────────────┬───────────────────────┘
                   │
                   ▼
    ┌──────────────────────────────────────┐
    │  4. FORWARD REQUEST                  │
    │  to ACCOUNT-SERVICE                  │
    │                                      │
    │  GET http://192.168.1.100:8081/      │
    │      get-account/ACC123              │
    │  Authorization: Bearer <JWT>         │
    └──────────────┬───────────────────────┘
                   │
                   ▼
         ┌─────────────────────┐
         │  ACCOUNT-SERVICE    │
         │  Processing...      │
         │  Retrieving Account │
         │  Details...         │
         └──────────┬──────────┘
                    │
                    ▼
    ┌──────────────────────────────────────┐
    │  5. SERVICE RESPONSE                 │
    │  HTTP 200 OK                         │
    │  {                                   │
    │    "accountId": "ACC123",            │
    │    "balance": 50000.00,              │
    │    "status": "ACTIVE",               │
    │    ...                               │
    │  }                                   │
    └──────────────┬───────────────────────┘
                   │
                   ▼
    ┌──────────────────────────────────────┐
    │  6. RESPONSE PROCESSING              │
    │  - Headers transformation            │
    │  - Body encoding                     │
    │  - Status code mapping               │
    └──────────────┬───────────────────────┘
                   │
                   ▼
    ┌──────────────────────────────────────┐
    │  7. SEND TO CLIENT                   │
    │  HTTP 200 OK                         │
    │  {                                   │
    │    "accountId": "ACC123",            │
    │    "balance": 50000.00,              │
    │    "status": "ACTIVE",               │
    │    ...                               │
    │  }                                   │
    └──────────────┬───────────────────────┘
                   │
                   ▼
    ┌──────────────────────────┐
    │   CLIENT RECEIVES DATA   │
    └──────────────────────────┘
```

---

## ❌ Error Handling

### Gateway Error Responses

```
Error Case 1: Missing JWT Token
┌─────────────────────────────┐
│ Request                     │
│ GET /account/get-account    │
│ (no Authorization header)   │
└─────────────────────────────┘
           │
           ▼
┌─────────────────────────────┐
│ JWT Authentication Filter   │
│ Token check: FAIL           │
└─────────────────────────────┘
           │
           ▼
Response (401 Unauthorized):
{
  "timestamp": "2026-03-01T10:30:00.000Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authorization header is missing or Invalid!",
  "path": "/account/get-account"
}

Error Case 2: Invalid/Expired JWT Token
┌─────────────────────────────┐
│ Request                     │
│ GET /account/get-account    │
│ Authorization: Bearer <expired_token>
└─────────────────────────────┘
           │
           ▼
┌─────────────────────────────┐
│ JWT Authentication Filter   │
│ ├─ Extract token            │
│ ├─ Verify signature: FAIL   │
│ └─ Token invalid or expired │
└─────────────────────────────┘
           │
           ▼
Response (401 Unauthorized):
{
  "timestamp": "2026-03-01T10:30:00.000Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token is invalid or expired",
  "path": "/account/get-account"
}

Error Case 3: Service Not Found
┌─────────────────────────────┐
│ Request                     │
│ GET /account/get-account    │
└─────────────────────────────┘
           │
           ▼
┌─────────────────────────────┐
│ Route Matching              │
│ Match: /account/** → Found  │
│ Service: ACCOUNT-SERVICE    │
│ Eureka Lookup: FAIL (no     │
│                instances)   │
└─────────────────────────────┘
           │
           ▼
Response (503 Service Unavailable):
{
  "timestamp": "2026-03-01T10:30:00.000Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "ACCOUNT-SERVICE is not available",
  "path": "/account/get-account"
}

Error Case 4: Route Not Found
┌─────────────────────────────┐
│ Request                     │
│ GET /unknown/endpoint       │
└─────────────────────────────┘
           │
           ▼
┌─────────────────────────────┐
│ Route Matching              │
│ No route matches /unknown/  │
│ Unknown path                │
└─────────────────────────────┘
           │
           ▼
Response (404 Not Found):
{
  "timestamp": "2026-03-01T10:30:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "No route found for path /unknown/endpoint",
  "path": "/unknown/endpoint"
}
```

---

## ⚙️ Configuration & Setup

### 1. Environment Variables

```bash
# Eureka Configuration
export EUREKA_URL=http://localhost:8761/eureka

# JWT Configuration
export JWT_SECRET_KEY=3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b
export JWT_EXPIRATION_TIME=86400000

# Gateway Configuration
export GATEWAY_PORT=8080
export AUTH_SERVER_URL=http://localhost:8085

# Logging Level
export LOG_LEVEL=INFO
```

### 2. Application Properties Configuration

```properties
# Gateway Server
spring.application.name=API-GATEWAY
server.port=8080

# Eureka Discovery
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.prefer-ip-address=true

# Gateway Routes
# AUTH-SERVER (Direct)
spring.cloud.gateway.routes[0].id=AUTH-SERVER
spring.cloud.gateway.routes[0].uri=http://localhost:8085
spring.cloud.gateway.routes[0].predicates[0]=Path=/auth-server/**
spring.cloud.gateway.routes[0].filters[0]=StripPrefix=1

# ACCOUNT-SERVICE (Eureka)
spring.cloud.gateway.routes[1].id=ACCOUNT-SERVICE
spring.cloud.gateway.routes[1].uri=lb://ACCOUNT-SERVICE
spring.cloud.gateway.routes[1].predicates[0]=Path=/account/**
spring.cloud.gateway.routes[1].filters[0]=StripPrefix=1

# TRANSACTION-SERVICE (Eureka)
spring.cloud.gateway.routes[2].id=TRANSACTION-SERVICE
spring.cloud.gateway.routes[2].uri=lb://TRANSACTION-SERVICE
spring.cloud.gateway.routes[2].predicates[0]=Path=/transaction/**
spring.cloud.gateway.routes[2].filters[0]=StripPrefix=1

# JWT Configuration
security.jwt.secret-key=3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b

# Service Discovery
spring.cloud.gateway.discovery.locator.enabled=true
spring.cloud.gateway.discovery.locator.lower-case-service-id=true

# Miscellaneous
spring.main.allow-bean-definition-overriding=true
spring.cloud.gateway.httpclient.ssl.use-insecure-trust-manager=true
```

---

## 🧪 Testing Integration

### 1. Test Authentication Flow

```bash
# Step 1: Login
curl -X POST http://localhost:8080/auth-server/unsecure/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass"}'

# Expected: JWT token in response
# Response:
# {
#   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "userId": "user123"
# }

# Step 2: Save token
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Step 3: Make authenticated request
curl -X GET http://localhost:8080/account/get-account/ACC123 \
  -H "Authorization: Bearer $TOKEN"
```

### 2. Test Service Discovery

```bash
# Check Eureka registry
curl http://localhost:8761/eureka/apps

# Check specific service
curl http://localhost:8761/eureka/apps/ACCOUNT-SERVICE

# Check instance health
curl http://localhost:8761/eureka/apps/ACCOUNT-SERVICE/ACCOUNT-SERVICE:8081/status
```

### 3. Integration Test Cases

| Test Case | Steps | Expected Result |
|-----------|-------|-----------------|
| **Login & Access Account** | 1. Login 2. Use token to get account | 200 OK with account data |
| **Token Expiration** | 1. Wait for token to expire 2. Make request | 401 Unauthorized |
| **Service Down** | 1. Stop ACCOUNT-SERVICE 2. Make request | 503 Service Unavailable |
| **Route Not Found** | 1. Call invalid path | 404 Not Found |
| **Transaction Processing** | 1. Create transfer 2. Check history | 200 OK with transaction details |

---

## 🐛 Troubleshooting

### Common Integration Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| **Service not discovered** | Eureka down/service not registered | Check Eureka at :8761, restart services |
| **401 errors on all requests** | JWT secret mismatch | Verify secret in AUTH-SERVER and GATEWAY |
| **404 on valid path** | Route configuration missing | Add route in application.properties |
| **Service timeout** | Service unreachable | Check service is running and healthy in Eureka |
| **CORS errors** | CORS headers missing | Configure CORS in SecurityConfig |
| **Auth header not forwarded** | AuthHeaderForwardFilter issue | Check filter configuration |

---

**Document Status**: Complete  
**Last Updated**: March 1, 2026  
**Version**: 1.0

