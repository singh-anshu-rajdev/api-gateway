# API Gateway - Architectural Design Document

**Smart Bank Elite System - Microservices Architecture**

## 📑 Document Information

- **Document Title**: API Gateway Architectural Design
- **Version**: 1.0
- **Last Updated**: March 2026
- **Status**: Active
- **Author**: Smart Bank Elite Development Team

---

## 🎯 1. Executive Summary

The API Gateway is the central hub of the Smart Bank Elite System, serving as the single entry point for all client requests. It implements the **Gateway Pattern** to provide unified access to multiple microservices, handling cross-cutting concerns like authentication, routing, logging, and load balancing.

### Key Responsibilities:
- Route client requests to appropriate microservices
- Validate JWT authentication tokens
- Manage service discovery via Eureka
- Implement global logging and error handling
- Balance load across service instances
- Maintain request/response integrity

---

## 🏗️ 2. System Architecture Overview

### 2.1 High-Level Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                        CLIENT APPLICATIONS                           │
│  (Web Browser, Mobile Apps, External APIs, Third-party Services)    │
└────────────────────────────┬─────────────────────────────────────────┘
                             │
                             │ HTTP/HTTPS Requests
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    API GATEWAY (Port: 8080)                          │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │            FILTER CHAIN (Request Processing)                  │  │
│  ├────────────────────────────────────────────────────────────────┤  │
│  │  1. Logging Filter                                            │  │
│  │     └─ Logs incoming request URI, method, and timestamp       │  │
│  │                                                                │  │
│  │  2. JWT Authentication Filter                                 │  │
│  │     └─ Validates Bearer token                                 │  │
│  │     └─ Allows /unsecure/ endpoints without token              │  │
│  │     └─ Returns 401 if token is missing or invalid             │  │
│  │                                                                │  │
│  │  3. Auth Header Forward Filter                                │  │
│  │     └─ Forwards Authorization header to downstream services   │  │
│  │                                                                │  │
│  │  4. Routing Logic                                             │  │
│  │     └─ Routes request to appropriate service based on path    │  │
│  │     └─ StripPrefix removes gateway prefix                     │  │
│  │     └─ Load balancing via Eureka                              │  │
│  │                                                                │  │
│  │  5. Global Exception Handler                                  │  │
│  │     └─ Catches and formats all exceptions                     │  │
│  │     └─ Returns appropriate error responses                    │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │         ROUTING CONFIGURATION (application.properties)        │  │
│  ├────────────────────────────────────────────────────────────────┤  │
│  │  Route 1: /auth-server/**      → AUTH-SERVER (8085)          │  │
│  │  Route 2: /account/**          → ACCOUNT-SERVICE (Eureka)    │  │
│  │  Route 3: /transaction/**      → TRANSACTION-SERVICE (Eureka)│  │
│  │  Route 4: [Future Routes]      → Other Services (Eureka)     │  │
│  └────────────────────────────────────────────────────────────────┘  │
└────────────────────────┬──────────────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┬──────────────────┐
         │               │               │                  │
         ▼               ▼               ▼                  ▼
    ┌─────────┐  ┌──────────────┐  ┌────────────┐  ┌─────────────────┐
    │AUTH-SRV │  │ ACCOUNT-SRV  │  │TRANS-SRV   │  │ [Other Services]│
    │ :8085   │  │ (Eureka)     │  │ (Eureka)   │  │  (Eureka)       │
    └────┬────┘  └──────┬───────┘  └──────┬─────┘  └────────┬────────┘
         │               │                │                │
         └───────────────┴────────────────┴────────────────┘
                         │
                         ▼
                 ┌─────────────────┐
                 │  Eureka Server  │
                 │  (Service       │
                 │   Discovery)    │
                 │  :8761          │
                 └─────────────────┘
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
         ┌─────────────┐     ┌──────────────┐
         │  Databases  │     │ External     │
         │  (For each  │     │ Integrations │
         │   Service)  │     │              │
         └─────────────┘     └──────────────┘
```

### 2.2 Component Architecture

```
API GATEWAY COMPONENTS
│
├── Spring Boot Application
│   ├── ApiGatewayApplication (Main Entry Point)
│   └── Spring Cloud Gateway (Routing Engine)
│
├── Filters & Middleware
│   ├── LoggingFilter
│   ├── JwtAuthenticationFilter
│   ├── AuthHeaderForwardFilter
│   └── Spring Global Filter Chain
│
├── Security & Authentication
│   ├── SecurityConfig
│   ├── JWT Token Validation
│   └── OAuth2 Resource Server
│
├── Routing & Discovery
│   ├── Route Configuration
│   ├── Eureka Discovery Client
│   └── Load Balancing
│
├── Exception Handling
│   ├── GlobalExceptionHandler
│   └── Custom Error Responses
│
└── Configuration
    ├── application.properties
    ├── Spring Cloud Config
    └── Environment Variables
```

---

## 3. Integration with Microservices

### 3.1 Service Topology

The API Gateway connects to and routes traffic to the following microservices:

#### **3.1.1 AUTH-SERVER (Authentication Service)**

```
┌─────────────────────────────────────────────────────────────┐
│                     AUTH-SERVER                             │
├─────────────────────────────────────────────────────────────┤
│ Port: 8085 (Direct) or via Eureka                          │
│ Service Name: AUTH-SERVER                                  │
│ Base URL: http://localhost:8085                            │
│ Gateway Path: /auth-server/**                              │
└─────────────────────────────────────────────────────────────┘

Responsibilities:
- User authentication and validation
- JWT token generation and signing
- User registration and account management
- Password reset and recovery
- Token refresh and validation

Key Endpoints:
┌────────────────────────────────────────────────────────────┐
│ POST   /unsecure/login                                     │
│        Request:  { "username": "string", "password": ... } │
│        Response: { "token": "JWT_TOKEN", "user": {...} }   │
├────────────────────────────────────────────────────────────┤
│ POST   /unsecure/register                                  │
│        Request:  { "username": "...", "password": "..." }  │
│        Response: { "userId": "...", "token": "..." }       │
├────────────────────────────────────────────────────────────┤
│ POST   /validate-token                                     │
│        Secured: Requires JWT token                         │
│        Response: { "valid": true, "user": {...} }          │
├────────────────────────────────────────────────────────────┤
│ POST   /refresh-token                                      │
│        Secured: Requires JWT token                         │
│        Response: { "token": "NEW_JWT_TOKEN" }              │
└────────────────────────────────────────────────────────────┘

Communication Flow:
Client → API-GATEWAY → AUTH-SERVER
         (JWT Validation → Pass through or validate)

Example Request Flow:
1. Client: POST /auth-server/unsecure/login with credentials
2. Gateway: LoggingFilter logs request
3. Gateway: JwtAuthenticationFilter skips (unsecure endpoint)
4. Gateway: Routes to AUTH-SERVER at /login
5. AuthServer: Validates credentials, generates JWT
6. Gateway: AuthHeaderForwardFilter forwards response
7. Client: Receives JWT token
```

#### **3.1.2 ACCOUNT-SERVICE**

```
┌─────────────────────────────────────────────────────────────┐
│                   ACCOUNT-SERVICE                           │
├─────────────────────────────────────────────────────────────┤
│ Port: Dynamic (via Eureka Discovery)                       │
│ Service Name: ACCOUNT-SERVICE                              │
│ Gateway Path: /account/**                                  │
│ Eureka Registration: http://localhost:8761/eureka          │
└─────────────────────────────────────────────────────────────┘

Responsibilities:
- Account creation and management
- Account balance tracking
- Account details and information
- Account holder information
- Account status management

Key Endpoints:
┌────────────────────────────────────────────────────────────┐
│ GET    /get-account/{accountId}                            │
│        Secured: Requires JWT token                         │
│        Response: { "accountId": "...", "balance": 5000 }   │
├────────────────────────────────────────────────────────────┤
│ POST   /create-account                                     │
│        Secured: Requires JWT token                         │
│        Request:  { "accountType": "SAVINGS", ...}          │
│        Response: { "accountId": "...", ... }               │
├────────────────────────────────────────────────────────────┤
│ PUT    /update-account/{accountId}                         │
│        Secured: Requires JWT token                         │
│        Request:  { "accountName": "...", ... }             │
│        Response: { "accountId": "...", ... }               │
├────────────────────────────────────────────────────────────┤
│ GET    /list-accounts                                      │
│        Secured: Requires JWT token                         │
│        Response: [ { "accountId": "..." }, ... ]           │
└────────────────────────────────────────────────────────────┘

Communication Flow:
Client → API-GATEWAY → ACCOUNT-SERVICE (via Eureka LB)
         (JWT Validation, Header Forward, Routing)

Example Request Flow:
1. Client: GET /account/get-account/ACC123 with JWT token
2. Gateway: LoggingFilter logs request
3. Gateway: JwtAuthenticationFilter validates JWT
4. Gateway: AuthHeaderForwardFilter adds auth header
5. Gateway: Eureka loads ACCOUNT-SERVICE instance
6. Gateway: Load balances to ACCOUNT-SERVICE
7. Gateway: Routes to /get-account/ACC123 (StripPrefix=/account)
8. AccountService: Retrieves account details
9. Client: Receives account information
```

#### **3.1.3 TRANSACTION-SERVICE**

```
┌─────────────────────────────────────────────────────────────┐
│                 TRANSACTION-SERVICE                         │
├─────────────────────────────────────────────────────────────┤
│ Port: Dynamic (via Eureka Discovery)                       │
│ Service Name: TRANSACTION-SERVICE                          │
│ Gateway Path: /transaction/**                              │
│ Eureka Registration: http://localhost:8761/eureka          │
└─────────────────────────────────────────────────────────────┘

Responsibilities:
- Transaction processing (transfers, payments)
- Transaction history and records
- Transaction status tracking
- Transaction validation and approval
- Audit trail maintenance

Key Endpoints:
┌────────────────────────────────────────────────────────────┐
│ POST   /transfer                                           │
│        Secured: Requires JWT token                         │
│        Request:  { "from": "ACC1", "to": "ACC2", ... }     │
│        Response: { "transactionId": "...", "status": ... } │
├────────────────────────────────────────────────────────────┤
│ GET    /history/{accountId}                                │
│        Secured: Requires JWT token                         │
│        Response: [ { "transactionId": "...", ... }, ... ]  │
├────────────────────────────────────────────────────────────┤
│ GET    /details/{transactionId}                            │
│        Secured: Requires JWT token                         │
│        Response: { "transactionId": "...", ... }           │
├────────────────────────────────────────────────────────────┤
│ PUT    /approve/{transactionId}                            │
│        Secured: Requires JWT token + Admin role            │
│        Response: { "transactionId": "...", "status": ... } │
└────────────────────────────────────────────────────────────┘

Communication Flow:
Client → API-GATEWAY → TRANSACTION-SERVICE (via Eureka LB)
         (JWT Validation, Header Forward, Routing)
           ↓
      Can also call ACCOUNT-SERVICE for balance updates

Example Request Flow:
1. Client: POST /transaction/transfer with JWT token
2. Gateway: LoggingFilter logs request
3. Gateway: JwtAuthenticationFilter validates JWT
4. Gateway: AuthHeaderForwardFilter adds auth header
5. Gateway: Eureka loads TRANSACTION-SERVICE instance
6. Gateway: Load balances to TRANSACTION-SERVICE
7. Gateway: Routes to /transfer (StripPrefix=/transaction)
8. TransactionService: Processes transfer
9. TransactionService: May call ACCOUNT-SERVICE for updates
10. Client: Receives transaction confirmation
```

#### **3.1.4 EUREKA SERVER (Service Discovery)**

```
┌─────────────────────────────────────────────────────────────┐
│                   EUREKA SERVER                             │
├─────────────────────────────────────────────────────────────┤
│ Port: 8761                                                 │
│ URL: http://localhost:8761                                │
│ Dashboard: http://localhost:8761                           │
│ Service Registry: http://localhost:8761/eureka             │
└─────────────────────────────────────────────────────────────┘

Responsibilities:
- Service registration and discovery
- Health check monitoring
- Dynamic instance management
- Service metadata management

API Gateway Integration:
┌────────────────────────────────────────────────────────────┐
│ Registry Configuration:                                    │
│                                                            │
│ spring.cloud.gateway.discovery.locator.enabled=true      │
│ spring.cloud.gateway.discovery.locator.lower-case-       │
│   service-id=true                                         │
│                                                            │
│ Eureka Client Config:                                     │
│                                                            │
│ eureka.client.service-url.defaultZone=                   │
│   http://localhost:8761/eureka                           │
│ eureka.client.register-with-eureka=true                  │
│ eureka.client.fetch-registry=true                        │
└────────────────────────────────────────────────────────────┘

Service Registration Format:
{
  "instance": {
    "instanceId": "ACCOUNT-SERVICE:8081",
    "app": "ACCOUNT-SERVICE",
    "ipAddr": "192.168.1.100",
    "port": 8081,
    "healthCheckUrl": "http://...:8081/actuator/health",
    "statusPageUrl": "http://...:8081/actuator",
    "metadata": {
      "version": "1.0"
    }
  }
}
```

### 3.2 Service Communication Patterns

#### **3.2.1 Request Flow - Secured Endpoint**

```
┌─────────┐
│ Client  │
└────┬────┘
     │
     │ 1. Request with JWT token
     │ GET /account/get-account/123
     │ Authorization: Bearer eyJhbGc...
     │
     ▼
┌────────────────────────────────────────────────┐
│         API GATEWAY (Port 8080)                │
├────────────────────────────────────────────────┤
│                                                │
│  ┌──────────────────────────────────────────┐ │
│  │ 2. LoggingFilter                         │ │
│  │    └─ Log: "Gateway received request:    │ │
│  │       /account/get-account/123"          │ │
│  └──────────────────────────────────────────┘ │
│         ↓                                      │
│  ┌──────────────────────────────────────────┐ │
│  │ 3. JwtAuthenticationFilter               │ │
│  │    ├─ Extract token from header          │ │
│  │    ├─ Validate token format              │ │
│  │    ├─ Check if /unsecure/ (No, continue)│ │
│  │    └─ Token valid ✓ Proceed             │ │
│  └──────────────────────────────────────────┘ │
│         ↓                                      │
│  ┌──────────────────────────────────────────┐ │
│  │ 4. AuthHeaderForwardFilter               │ │
│  │    └─ Add Authorization header to        │ │
│  │       downstream request                 │ │
│  └──────────────────────────────────────────┘ │
│         ↓                                      │
│  ┌──────────────────────────────────────────┐ │
│  │ 5. Route Matching & Load Balancing       │ │
│  │    ├─ Match path /account/**             │ │
│  │    ├─ Query Eureka for ACCOUNT-SERVICE  │ │
│  │    ├─ Select instance (load balance)    │ │
│  │    ├─ StripPrefix=/account              │ │
│  │    └─ Forward to /get-account/123       │ │
│  └──────────────────────────────────────────┘ │
│                                                │
└────────────┬───────────────────────────────────┘
             │
             │ 6. Routed request with auth header
             │
             ▼
        ┌─────────────────────┐
        │  ACCOUNT-SERVICE    │
        │  (Instance: 8081)   │
        ├─────────────────────┤
        │ 7. Process request  │
        │    - Validate JWT   │
        │    - Get account    │
        │ 8. Return response  │
        └────────┬────────────┘
                 │
                 │ 9. Response with data
                 │ { "accountId": "123", ... }
                 │
                 ▼
        ┌─────────────────────┐
        │   API GATEWAY       │
        │   - Log response    │
        │   - Format response │
        └────────┬────────────┘
                 │
                 │ 10. Final response to client
                 │
                 ▼
        ┌──────────────┐
        │ Client       │
        │ Receives:    │
        │ { account..} │
        └──────────────┘
```

#### **3.2.2 Request Flow - Unsecured Endpoint (Login)**

```
┌─────────┐
│ Client  │
└────┬────┘
     │
     │ 1. Request without token
     │ POST /auth-server/unsecure/login
     │ { "username": "user", "password": "pass" }
     │
     ▼
┌────────────────────────────────────────────────┐
│         API GATEWAY (Port 8080)                │
├────────────────────────────────────────────────┤
│                                                │
│  ┌──────────────────────────────────────────┐ │
│  │ LoggingFilter                            │ │
│  │ └─ Log request                           │ │
│  └──────────────────────────────────────────┘ │
│         ↓                                      │
│  ┌──────────────────────────────────────────┐ │
│  │ JwtAuthenticationFilter                  │ │
│  │ ├─ Check path: /auth-server/unsecure/   │ │
│  │ ├─ Contains "/unsecure/" ? YES           │ │
│  │ └─ Skip validation, proceed ✓            │ │
│  └──────────────────────────────────────────┘ │
│         ↓                                      │
│  ┌──────────────────────────────────────────┐ │
│  │ Route Matching                           │ │
│  │ ├─ Match path /auth-server/**            │ │
│  │ ├─ URI: http://localhost:8085            │ │
│  │ ├─ StripPrefix=/auth-server              │ │
│  │ └─ Forward to /unsecure/login            │ │
│  └──────────────────────────────────────────┘ │
│                                                │
└────────────┬───────────────────────────────────┘
             │
             │ 2. Forward to AUTH-SERVER
             │
             ▼
        ┌─────────────────────┐
        │  AUTH-SERVER        │
        │  (Port 8085)        │
        ├─────────────────────┤
        │ 3. Receive request  │
        │    - Validate creds │
        │    - Generate JWT   │
        │ 4. Return token     │
        │    { "token": ... } │
        └────────┬────────────┘
                 │
                 │ 5. Response with JWT
                 │
                 ▼
        ┌─────────────────────┐
        │   API GATEWAY       │
        │   - Log response    │
        │   - Forward response│
        └────────┬────────────┘
                 │
                 │ 6. Final response
                 │
                 ▼
        ┌──────────────────────┐
        │ Client               │
        │ Receives: JWT token  │
        │ Stores in LocalStore │
        └──────────────────────┘
```

---

## 4. Technical Architecture

### 4.1 Technology Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Runtime** | Java | 21+ | Application runtime |
| **Framework** | Spring Boot | 3.5.11 | Application framework |
| **Cloud** | Spring Cloud | 2025.0.1 | Microservices patterns |
| **Gateway** | Spring Cloud Gateway | Latest | Request routing |
| **Discovery** | Eureka Client | Netflix | Service discovery |
| **Security** | Spring Security | 3.5.11 | Authentication & authorization |
| **Auth** | OAuth2/JWT | JJWT 0.11.5 | Token-based auth |
| **Build Tool** | Maven | 3.6.0+ | Build automation |
| **HTTP Client** | Netty/Reactor | Reactive | Non-blocking HTTP |

### 4.2 Dependency Graph

```
Spring Boot 3.5.11 (Parent)
├── spring-boot-starter-security
│   └── Spring Security Core
├── spring-boot-starter-oauth2-resource-server
│   └── OAuth2 Resource Server
├── spring-cloud-starter-gateway
│   ├── Spring Cloud Gateway
│   ├── Spring Cloud Commons
│   ├── Spring WebFlux (Reactive)
│   └── Netty (HTTP Client)
├── spring-cloud-starter-netflix-eureka-client
│   ├── Eureka Client
│   ├── Ribbon (Load Balancer)
│   └── Service Discovery
├── jjwt-api (0.11.5)
│   ├── jjwt-impl
│   └── jjwt-jackson
└── spring-boot-starter-test
    └── Testing frameworks
```

### 4.3 Reactive Processing Pipeline

The gateway uses Spring WebFlux for reactive, non-blocking request processing:

```
┌─────────────────────────────────────────────────────────┐
│              CLIENT REQUEST                             │
│  (HTTP Request with Body & Headers)                    │
└────────────┬────────────────────────────────────────────┘
             │
             ▼
    ┌─────────────────────┐
    │  Netty Server       │
    │  (Non-blocking)     │
    └────────┬────────────┘
             │
             ▼
    ┌──────────────────────────────────────┐
    │  ServerWebExchange                   │
    │  - ServerHttpRequest                 │
    │  - ServerHttpResponse                │
    │  - WebSession                        │
    │  - Principal                         │
    └────────┬─────────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────────┐
    │  Filter Chain (Mono<Void>)           │
    │                                      │
    │  LoggingFilter                       │
    │    ↓ (Mono chain)                    │
    │  JwtAuthenticationFilter             │
    │    ↓ (Mono chain)                    │
    │  AuthHeaderForwardFilter             │
    │    ↓ (Mono chain)                    │
    │  Gateway Route Filters               │
    │    ↓ (Mono chain)                    │
    │  Route to Service (WebClient)        │
    │    ↓ (Mono chain)                    │
    │  Response Processing                 │
    └────────┬─────────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────────┐
    │  Downstream Service                  │
    │  (HTTP Request)                      │
    │                                      │
    │  Processing...                       │
    │                                      │
    │  (HTTP Response)                     │
    └────────┬─────────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────────┐
    │  Response Processing Chain           │
    │  (Reverse order)                     │
    │                                      │
    │  Response Transformation             │
    │    ↑ (Mono chain)                    │
    │  Headers Management                  │
    │    ↑ (Mono chain)                    │
    │  Response Building                   │
    └────────┬─────────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────────┐
    │  ServerHttpResponse                  │
    │  - HTTP Status Code                  │
    │  - Response Headers                  │
    │  - Response Body                     │
    └────────┬─────────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────────┐
    │  Netty Serialization                 │
    │  (Convert to bytes)                  │
    └────────┬─────────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────────┐
    │  Network (TCP/IP)                    │
    └────────┬─────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  CLIENT RECEIVES RESPONSE               │
│  (HTTP Response with Body & Headers)    │
└─────────────────────────────────────────┘
```

---

## 5. Routing Configuration

### 5.1 Route Definition Structure

```yaml
spring.cloud.gateway.routes[index]:
  id: UNIQUE_ROUTE_ID          # Unique identifier
  uri: target-service-url      # Destination (http:// or lb://)
  predicates:                  # Request matching conditions
    - Path=/path/**            # URL path pattern
  filters:                      # Request/response transformations
    - StripPrefix=1            # Remove prefix from path
    - AuthHeader=              # Custom headers
```

### 5.2 Current Routes Configuration

```properties
# Route 0: AUTH-SERVER
spring.cloud.gateway.routes[0].id=AUTH-SERVER
spring.cloud.gateway.routes[0].uri=http://localhost:8085
spring.cloud.gateway.routes[0].predicates[0]=Path=/auth-server/**
spring.cloud.gateway.routes[0].filters[0]=StripPrefix=1

# Route 1: ACCOUNT-SERVICE
spring.cloud.gateway.routes[1].id=ACCOUNT-SERVICE
spring.cloud.gateway.routes[1].uri=lb://ACCOUNT-SERVICE
spring.cloud.gateway.routes[1].predicates[0]=Path=/account/**
spring.cloud.gateway.routes[1].filters[0]=StripPrefix=1

# Route 2: TRANSACTION-SERVICE
spring.cloud.gateway.routes[2].id=TRANSACTION-SERVICE
spring.cloud.gateway.routes[2].uri=lb://TRANSACTION-SERVICE
spring.cloud.gateway.routes[2].predicates[0]=Path=/transaction/**
spring.cloud.gateway.routes[2].filters[0]=StripPrefix=1
```

### 5.3 Route Matching Logic

```
Request: GET /account/get-account/123

                    ↓
         
┌─────────────────────────────────────┐
│  Check Route 0 (/auth-server/**)    │
│  Path /account/... → NO MATCH       │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  Check Route 1 (/account/**)        │
│  Path /account/... → MATCH ✓        │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  Eureka Lookup: ACCOUNT-SERVICE     │
│  - Query Eureka server              │
│  - Get available instances          │
│  - Load balance (round-robin)       │
│  - Select: 192.168.1.100:8081       │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  Apply Filters                      │
│  - StripPrefix=1                    │
│  - Remove /account                  │
│  - New path: /get-account/123       │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  Forward Request                    │
│  http://192.168.1.100:8081/         │
│         get-account/123             │
└─────────────────────────────────────┘
         │
         ▼
   ACCOUNT-SERVICE processes request
```

### 5.4 Adding New Routes

To add a new service route, follow these steps:

#### **Step 1: Add Route Configuration**

In `application.properties`:

```properties
# Route 3: NEW-SERVICE
spring.cloud.gateway.routes[3].id=NEW-SERVICE
spring.cloud.gateway.routes[3].uri=lb://NEW-SERVICE
spring.cloud.gateway.routes[3].predicates[0]=Path=/new-service/**
spring.cloud.gateway.routes[3].filters[0]=StripPrefix=1
```

#### **Step 2: Ensure Service Registers with Eureka**

In the new service's `application.properties`:

```properties
spring.application.name=NEW-SERVICE
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
```

#### **Step 3: Test the Route**

```bash
curl http://localhost:8080/new-service/endpoint \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### **Step 4: Restart Gateway** (if needed)

```bash
./mvnw spring-boot:run
```

---

## 6. Security Architecture

### 6.1 JWT Token Structure

```
┌──────────────────────────────────────────────────────────┐
│                    JWT TOKEN                             │
│  eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.                 │
│  eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4E...       │
│  SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c            │
└──────────────────────────────────────────────────────────┘
         │                      │                 │
         ▼                      ▼                 ▼
    ┌─────────┐          ┌──────────┐        ┌──────────┐
    │ HEADER  │          │ PAYLOAD  │        │SIGNATURE │
    ├─────────┤          ├──────────┤        ├──────────┤
    │         │          │          │        │          │
    │ alg:    │          │ sub:     │        │ HMAC     │
    │ HS256   │          │ userId   │        │ SHA256   │
    │         │          │ exp:     │        │          │
    │ typ:    │          │ 1645651  │        │ secret-  │
    │ JWT     │          │ 200      │        │ key      │
    │         │          │ iat:     │        │          │
    │         │          │ 1645564  │        │          │
    │         │          │ 800      │        │          │
    │         │          │          │        │          │
    └─────────┘          └──────────┘        └──────────┘
    Base64URL            Base64URL            Base64URL
```

### 6.2 Authentication Flow

```
1. LOGIN REQUEST
   ┌─────────────────────────────────────┐
   │ Client                              │
   │ POST /auth-server/unsecure/login    │
   │ { "username": "...", "pwd": "..." } │
   └────────────┬────────────────────────┘
                │
                ▼
   ┌──────────────────────────────────────┐
   │ API Gateway                          │
   │ - LoggingFilter: Log request         │
   │ - JwtAuthenticationFilter: Skip      │
   │   (unsecure endpoint)                │
   │ - Route to AUTH-SERVER               │
   └────────────┬─────────────────────────┘
                │
                ▼
   ┌──────────────────────────────────────┐
   │ AUTH-SERVER                          │
   │ - Validate credentials               │
   │ - Create JWT token                   │
   │   { "sub": "user123",                │
   │     "exp": 1645651200,               │
   │     "iat": 1645564800 }              │
   │ - Sign with secret key               │
   │ - Return token to gateway            │
   └────────────┬─────────────────────────┘
                │
                ▼
   ┌──────────────────────────────────────┐
   │ API Gateway                          │
   │ - Return token to client             │
   └────────────┬─────────────────────────┘
                │
                ▼
   ┌──────────────────────────────────────┐
   │ Client                               │
   │ Receives JWT and stores locally      │
   │ (localStorage, cookie, etc.)         │
   └──────────────────────────────────────┘

2. AUTHENTICATED REQUEST
   ┌──────────────────────────────────────┐
   │ Client                               │
   │ GET /account/details                 │
   │ Authorization: Bearer eyJhbGc...     │
   └────────────┬─────────────────────────┘
                │
                ▼
   ┌──────────────────────────────────────┐
   │ API Gateway                          │
   │ - LoggingFilter: Log request         │
   │ - JwtAuthenticationFilter:           │
   │   * Extract token from header        │
   │   * Validate format (Bearer ...)     │
   │   * Check expiration                 │
   │   * Verify signature                 │
   │   * Token valid? YES → Continue      │
   │ - AuthHeaderForwardFilter:           │
   │   * Forward auth header              │
   │ - Route to ACCOUNT-SERVICE           │
   └────────────┬─────────────────────────┘
                │
                ▼
   ┌──────────────────────────────────────┐
   │ ACCOUNT-SERVICE                      │
   │ - Receives request with token        │
   │ - May re-validate token with         │
   │   AUTH-SERVER (optional)             │
   │ - Return account details             │
   └────────────┬─────────────────────────┘
                │
                ▼
   ┌──────────────────────────────────────┐
   │ API Gateway                          │
   │ - Return response to client          │
   └────────────┬─────────────────────────┘
                │
                ▼
   ┌──────────────────────────────────────┐
   │ Client                               │
   │ Receives account data                │
   └──────────────────────────────────────┘

3. TOKEN EXPIRATION
   ┌──────────────────────────────────────┐
   │ Client                               │
   │ GET /account/details                 │
   │ Authorization: Bearer expired_token  │
   └────────────┬─────────────────────────┘
                │
                ▼
   ┌──────────────────────────────────────┐
   │ API Gateway                          │
   │ - JwtAuthenticationFilter:           │
   │   * Check token expiration           │
   │   * Token EXPIRED? YES               │
   │   * Return 401 Unauthorized          │
   │ - Block request                      │
   └────────────┬─────────────────────────┘
                │
                ▼
   ┌──────────────────────────────────────┐
   │ Client                               │
   │ Receives 401 Unauthorized            │
   │ Must login again or refresh token    │
   └──────────────────────────────────────┘
```

### 6.3 Security Configuration

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    // Current configuration:
    // - CSRF disabled (API-based, no form-based attacks)
    // - All exchanges permitted (auth handled by filters)
    // - Future: Add role-based access control (RBAC)
}
```

**Security Best Practices:**

1. ✅ **JWT Validation**: Required for secured endpoints
2. ✅ **Bearer Token Format**: Standard Authorization header
3. ✅ **Token Expiration**: Implemented at AUTH-SERVER
4. ✅ **Secret Key**: Configured in application.properties
5. ⚠️ **HTTPS**: Recommended for production
6. ⚠️ **CORS**: May need configuration for web clients
7. ⚠️ **Rate Limiting**: Consider adding for production

---

## 7. Error Handling & Exceptions

### 7.1 Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    // Handles all uncaught exceptions
    // Returns consistent error response format
    // Logs exceptions for monitoring
}
```

### 7.2 Error Response Format

```json
{
  "timestamp": "2026-03-01T10:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request parameters",
  "path": "/account/get-account"
}
```

### 7.3 HTTP Status Codes

| Code | Meaning | Example Scenario |
|------|---------|------------------|
| 200 | OK | Request successful |
| 201 | Created | Resource created |
| 400 | Bad Request | Invalid input |
| 401 | Unauthorized | Missing/invalid JWT |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Service/endpoint not found |
| 500 | Server Error | Gateway/service error |
| 503 | Service Unavailable | Service down/unreachable |

---

## 8. Monitoring & Observability

### 8.1 Logging

```java
// Implemented in LoggingFilter
System.out.println("Gateway received request: " + exchange.getRequest().getURI());

// Output example:
// Gateway received request: /account/get-account/123
// Gateway received request: /transaction/transfer
```

### 8.2 Metrics to Monitor

| Metric | Description | Tool |
|--------|-------------|------|
| Request Count | Total requests/second | Spring Boot Actuator |
| Response Time | Avg response latency | Spring Boot Actuator |
| Error Rate | 4xx, 5xx responses | Spring Boot Actuator |
| Service Health | Service availability | Eureka Dashboard |
| JWT Failures | Token validation failures | Application Logs |

### 8.3 Eureka Health Dashboard

Access at: `http://localhost:8761`

Shows:
- Registered service instances
- Service health status
- Instance metadata
- Last heartbeat timestamp

---

## 9. Deployment Architecture

### 9.1 Development Environment

```
Developer Machine
├── API Gateway (port 8080)
├── AUTH-SERVER (port 8085)
├── ACCOUNT-SERVICE (Eureka-discovered)
├── TRANSACTION-SERVICE (Eureka-discovered)
└── Eureka Server (port 8761)
```

### 9.2 Production Environment

```
Production Infrastructure
│
├── Load Balancer (Nginx/HAProxy)
│   │
│   └── Multiple Gateway Instances
│       ├── API Gateway Instance 1 (port 8080)
│       ├── API Gateway Instance 2 (port 8080)
│       └── API Gateway Instance 3 (port 8080)
│
├── Eureka Server Cluster
│   ├── Eureka Server 1 (port 8761)
│   └── Eureka Server 2 (port 8761)
│
├── Microservices Cluster
│   ├── AUTH-SERVER (multiple instances)
│   ├── ACCOUNT-SERVICE (multiple instances)
│   └── TRANSACTION-SERVICE (multiple instances)
│
├── Databases
│   ├── Auth DB
│   ├── Account DB
│   └── Transaction DB
│
└── Monitoring & Logging
    ├── ELK Stack / Splunk
    ├── Prometheus
    ├── Grafana
    └── Centralized Logging
```

---

## 10. Scalability Considerations

### 10.1 Horizontal Scaling

The gateway can be horizontally scaled by:

1. **Multiple Gateway Instances**
   ```bash
   # Instance 1
   java -jar apiGateway-0.0.1-SNAPSHOT.jar --server.port=8080
   
   # Instance 2
   java -jar apiGateway-0.0.1-SNAPSHOT.jar --server.port=8081
   
   # Instance 3
   java -jar apiGateway-0.0.1-SNAPSHOT.jar --server.port=8082
   ```

2. **Load Balancer Configuration** (Nginx example)
   ```nginx
   upstream gateway {
       server localhost:8080;
       server localhost:8081;
       server localhost:8082;
   }
   
   server {
       listen 80;
       location / {
           proxy_pass http://gateway;
       }
   }
   ```

3. **Service Discovery**
   - All instances register with Eureka
   - Eureka handles service metadata
   - Gateway discovers available instances automatically

### 10.2 Load Balancing Strategy

- **Default**: Round-robin via Ribbon
- **Custom**: Configurable via spring.cloud.loadbalancer

### 10.3 Performance Optimization

- **Caching**: Token validation caching
- **Connection Pooling**: HTTP client connection pooling
- **Compression**: Gzip response compression
- **Asynchronous Processing**: Reactive/non-blocking with WebFlux

---

## 11. Future Enhancements

### 11.1 Planned Features

- [ ] **Rate Limiting**: Token bucket algorithm
- [ ] **API Versioning**: Support multiple API versions
- [ ] **Circuit Breaker**: Resilience4j integration
- [ ] **Distributed Tracing**: Sleuth + Zipkin
- [ ] **API Documentation**: Swagger/OpenAPI integration
- [ ] **CORS Configuration**: Dynamic CORS handling
- [ ] **Request Validation**: Input validation middleware
- [ ] **Response Transformation**: Custom response formatters
- [ ] **Caching Layer**: Redis caching for tokens/responses
- [ ] **Audit Logging**: Detailed audit trail for compliance

### 11.2 Technology Upgrades

- [ ] Spring Boot 4.0+ (when released)
- [ ] Spring Cloud 2026+ versions
- [ ] Java 22+ support
- [ ] GraalVM native image support

---

## 12. Troubleshooting Guide

### 12.1 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| **Services not discovered** | Eureka server down | Start Eureka on 8761 |
| **JWT validation fails** | Secret key mismatch | Match secret in Auth & Gateway |
| **404 on valid endpoint** | Route not configured | Add route in application.properties |
| **Gateway timeout** | Service unreachable | Check service health in Eureka |
| **CORS errors** | No CORS headers | Configure CORS in SecurityConfig |
| **Port conflicts** | Port already in use | Change port in application.properties |

---

## 13. References & Resources

- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Netflix Eureka](https://github.com/Netflix/eureka)
- [JWT (jwt.io)](https://jwt.io)
- [Microservices Patterns](https://microservices.io/patterns/apigateway.html)

---

## 📞 Contact & Support

- **Project**: Smart Bank Elite System
- **Component**: API Gateway
- **Repository**: https://github.com/singh-anshu-rajdev/api-gateway
- **Maintainer**: Anshu Singh

---

**Document Version**: 1.0  
**Last Updated**: March 2026  
**Status**: Active & Production Ready

