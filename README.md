# API Gateway - Smart Bank Elite System

Welcome to the **API Gateway** service for the Smart Bank Elite System. This is the central entry point for all client requests, providing routing, authentication, and security features for the microservices ecosystem.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Configuration](#configuration)
- [Usage](#usage)
- [Connected Services](#connected-services)
- [API Endpoints](#api-endpoints)
- [Authentication](#authentication)
- [Filters & Middleware](#filters--middleware)
- [Development](#development)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## 🎯 Overview

The API Gateway serves as the **single entry point** for all client applications in the Smart Bank Elite System. It handles:

- **Request Routing**: Routes requests to appropriate microservices
- **Service Discovery**: Dynamically discovers services via Eureka
- **Authentication & Authorization**: Validates JWT tokens
- **Cross-Service Communication**: Manages communication between services
- **Request/Response Logging**: Tracks all incoming and outgoing requests
- **Load Balancing**: Distributes requests across service instances
- **Security**: Enforces HTTPS, validates tokens, and manages CORS

## 🏗️ Architecture

The API Gateway follows the **Gateway Pattern** in microservices architecture. For detailed architectural design, see [ARCHITECTURE.md](./ARCHITECTURE.md).

### High-Level Architecture

```
┌─────────────┐
│   Clients   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│      API GATEWAY (Spring Cloud)     │
│  ┌──────────────────────────────┐   │
│  │ Request Processing Pipeline  │   │
│  ├──────────────────────────────┤   │
│  │ 1. Logging Filter            │   │
│  │ 2. JWT Auth Filter           │   │
│  │ 3. Header Forward Filter     │   │
│  │ 4. Routing Logic             │   │
│  └──────────────────────────────┘   │
└────────┬────────────────────────────┘
         │
    ┌────┼────┬─────────────┬────────────┐
    ▼    ▼    ▼             ▼            ▼
┌─────┐ ┌──────────┐ ┌────────────┐ ┌──────────────┐
│AUTH │ │ ACCOUNT  │ │TRANSACTION │ │OTHER SERVICE │
└─────┘ └──────────┘ └────────────┘ └──────────────┘
    │    │          │             │            │
    └────┴──────────┴─────────────┴────────────┘
         │
         ▼
   ┌────────────┐
   │  EUREKA    │
   │ (Service   │
   │ Discovery) │
   └────────────┘
```

## ✨ Features

- ✅ **Dynamic Service Discovery**: Integrates with Eureka for automatic service registration and discovery
- ✅ **JWT Token Validation**: Validates JWT tokens for secured endpoints
- ✅ **Request Routing**: Intelligent routing based on URL patterns
- ✅ **Global Filters**: Logging, authentication, and header management
- ✅ **Load Balancing**: Built-in load balancing with Eureka
- ✅ **Exception Handling**: Centralized global exception handling
- ✅ **WebFlux Support**: Reactive, non-blocking request processing
- ✅ **Spring Boot 3.5.11**: Latest Spring Boot features
- ✅ **Security**: CSRF disabled for API, configurable security policies

## 📁 Project Structure

```
apiGateway/
├── src/
│   ├── main/
│   │   ├── java/com/smartBankElite/apiGateway/
│   │   │   ├── ApiGatewayApplication.java          # Main application class
│   │   │   ├── config/
│   │   │   │   ├── AuthHeaderForwardFilter.java    # Forwards auth headers to services
│   │   │   │   ├── GatewayUtils.java               # Utility functions
│   │   │   │   ├── JwtAuthenticationFilter.java    # JWT validation filter
│   │   │   │   ├── LoggingFilter.java              # Request/response logging
│   │   │   │   └── SecurityConfig.java             # Security configuration
│   │   │   └── ExceptionHandler/
│   │   │       └── GlobalExceptionHandler.java     # Global exception handling
│   │   └── resources/
│   │       └── application.properties              # Configuration
│   └── test/
│       └── java/.../ApiGatewayApplicationTests.java
├── pom.xml                                         # Maven dependencies
├── mvnw / mvnw.cmd                                 # Maven wrapper
├── ARCHITECTURE.md                                 # Detailed architecture
├── README.md                                       # This file
└── HELP.md
```

## 📦 Prerequisites

- **Java**: JDK 21 or higher
- **Maven**: 3.6.0 or higher (or use the included Maven wrapper)
- **Spring Boot**: 3.5.11
- **Eureka Server**: Running on `localhost:8761`
- **Git**: For version control

## 🚀 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/singh-anshu-rajdev/api-gateway.git
cd api-gateway
```

### 2. Build the Project

Using Maven wrapper (recommended):

```bash
./mvnw clean install
```

Or with your system Maven:

```bash
mvn clean install
```

### 3. Run the Application

Using Maven:

```bash
./mvnw spring-boot:run
```

Or directly with Java:

```bash
java -jar target/apiGateway-0.0.1-SNAPSHOT.jar
```

The gateway will start on **`http://localhost:8080`**

## ⚙️ Configuration

### application.properties

Key configuration properties:

```properties
# Application
spring.application.name=API-GATEWAY
server.port=8080

# Eureka Discovery
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true

# Routes Configuration
spring.cloud.gateway.routes[0].id=AUTH-SERVER
spring.cloud.gateway.routes[0].uri=http://localhost:8085
spring.cloud.gateway.routes[0].predicates[0]=Path=/auth-server/**

# JWT Secret (Change in production!)
security.jwt.secret-key=3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b
```

### Environment Variables

For production, set these environment variables:

```bash
EUREKA_URL=http://eureka-server:8761/eureka
JWT_SECRET_KEY=your-production-secret-key
GATEWAY_PORT=8080
```

## 🔀 Usage

### Making Requests Through the Gateway

All client requests should go through the gateway:

```bash
# Without authentication (unsecured endpoints)
curl http://localhost:8080/auth-server/unsecure/login \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass"}'

# With authentication (secured endpoints)
curl http://localhost:8080/account/get-account \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Adding New Routes

Edit `application.properties` to add new service routes:

```properties
# New route for a service
spring.cloud.gateway.routes[3].id=NEW-SERVICE
spring.cloud.gateway.routes[3].uri=lb://NEW-SERVICE
spring.cloud.gateway.routes[3].predicates[0]=Path=/new-service/**
spring.cloud.gateway.routes[3].filters[0]=StripPrefix=1
```

Or use Eureka-based discovery (recommended):

```properties
spring.cloud.gateway.discovery.locator.enabled=true
```

## 🔗 Connected Services

The API Gateway routes requests to the following microservices in the Smart Bank Elite System:

### 1. **AUTH-SERVER** (Authentication Service)
- **Port**: 8085 (Direct) or via Eureka
- **Base Path**: `/auth-server`
- **Purpose**: Handles user authentication and JWT token generation
- **Key Endpoints**:
  - `POST /auth-server/unsecure/login` - Login and get JWT token
  - `POST /auth-server/unsecure/register` - Register new user
  - `POST /auth-server/validate-token` - Validate JWT token

### 2. **ACCOUNT-SERVICE**
- **Port**: Discovered via Eureka
- **Base Path**: `/account`
- **Purpose**: Manages user accounts, profiles, and account operations
- **Key Endpoints**:
  - `GET /account/get-account/{accountId}` - Get account details
  - `POST /account/create-account` - Create new account
  - `PUT /account/update-account` - Update account information
  - `GET /account/list-accounts` - List all accounts

### 3. **TRANSACTION-SERVICE**
- **Port**: Discovered via Eureka
- **Base Path**: `/transaction`
- **Purpose**: Handles financial transactions between accounts
- **Key Endpoints**:
  - `POST /transaction/transfer` - Create a new transaction
  - `GET /transaction/history/{accountId}` - Get transaction history
  - `GET /transaction/details/{transactionId}` - Get transaction details
  - `PUT /transaction/approve` - Approve pending transaction

### 4. **EUREKA SERVER** (Service Registry)
- **Port**: 8761
- **URL**: `http://localhost:8761`
- **Purpose**: Service discovery and registration for all microservices
- **Dashboard**: http://localhost:8761/eureka

## 🔐 Authentication

### JWT Token Flow

1. **Client Login**: 
   ```bash
   POST /auth-server/unsecure/login
   ```
   Response: `{ "token": "eyJhbGc..." }`

2. **Use Token**: Add to subsequent requests
   ```bash
   Authorization: Bearer eyJhbGc...
   ```

3. **Gateway Validation**: JWT Authentication Filter validates the token
   - Checks if token is present and valid format
   - Validates token signature and expiration (via Auth Service)
   - Forwards request if valid, rejects if invalid

### Token Structure

```
Header: {
  "alg": "HS256",
  "typ": "JWT"
}

Payload: {
  "sub": "user123",
  "username": "john_doe",
  "iat": 1645564800,
  "exp": 1645651200
}

Signature: HMACSHA256(header.payload, secret-key)
```

## 🔀 Filters & Middleware

The gateway applies filters in the following order:

### 1. **LoggingFilter** (Order: -1)
- Logs all incoming requests
- Displays request URI and HTTP method
- Helps with debugging and monitoring

```java
Gateway received request: /account/get-account/123
Gateway received request: /transaction/transfer
```

### 2. **JwtAuthenticationFilter** (Order: -1)
- Validates JWT tokens for secured endpoints
- Allows `/unsecure/` endpoints without authentication
- Returns 401 Unauthorized if token is missing or invalid

### 3. **AuthHeaderForwardFilter**
- Forwards authorization headers to downstream services
- Ensures JWT token is passed through the chain

### 4. **Request Routing**
- Routes requests based on path predicates
- Strips prefix and forwards to target service

## 👨‍💻 Development

### Building Locally

```bash
./mvnw clean install
```

### Running Tests

```bash
./mvnw test
```

### Running with Debug Mode

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--debug"
```

### IDE Setup (IntelliJ IDEA)

1. Open the project folder
2. Maven dependencies will auto-load
3. Run `ApiGatewayApplication.java` as Java Application

## 🐛 Troubleshooting

### Issue: Gateway can't connect to Eureka

**Solution**: Ensure Eureka Server is running on `http://localhost:8761`

```bash
# Check Eureka availability
curl http://localhost:8761/eureka/apps
```

### Issue: Services not appearing in Eureka

**Solution**: Ensure each service is properly configured with Eureka client:

```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
```

### Issue: JWT Token Validation Fails

**Solution**: 
- Ensure JWT secret key matches in both Auth Service and Gateway
- Check token expiration time
- Verify token format starts with "Bearer "

### Issue: Route Not Working

**Solution**: 
- Check route configuration in `application.properties`
- Verify service is registered in Eureka: http://localhost:8761
- Check service URI is correct (use `lb://` for Eureka discovery)

### Issue: Port Already in Use

```bash
# Change port in application.properties
server.port=8081
```

## 📚 Related Documentation

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Eureka Service Discovery](https://github.com/Netflix/eureka)
- [JWT (JSON Web Tokens)](https://jwt.io)

## 🤝 Contributing

### Guidelines

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Commit changes: `git commit -am 'Add new feature'`
3. Push to branch: `git push origin feature/your-feature`
4. Submit a pull request

### Code Style

- Follow Spring Java conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Write unit tests for new features

## 📝 License

This project is part of Smart Bank Elite System. 

## 👤 Author

- **Developer**: Anshu Singh
- **Email**: singh.anshu.rajdev@example.com
- **GitHub**: [singh-anshu-rajdev](https://github.com/singh-anshu-rajdev)

## 📞 Support

For issues, questions, or suggestions:
1. Check existing GitHub issues
2. Create a new GitHub issue with detailed description
3. Contact the development team

---

**Last Updated**: March 2026  
**Version**: 0.0.1-SNAPSHOT

