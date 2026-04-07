<div align="center">

![PayFlow Banner](./assets/PayFlow.png)

# 💳 PayFlow

### Digital Wallet & Payment System

**A production-grade payment backend built with Microservices Architecture**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

[Getting Started](#-getting-started) · [API Docs](#-api-endpoints) · [Architecture](#-architecture) · [Design Decisions](#-design-decisions)

</div>

---

## 🎯 What is PayFlow?

PayFlow is a **digital wallet system** — think of it like a simplified version of **Paytm, PhonePe, or Google Pay's backend**.

**What can users do?**
- 📝 **Sign up & log in** securely (passwords are encrypted, sessions use JWT tokens)
- 💰 **Create a wallet** and add money to it
- 💸 **Send money** to other users instantly
- 📊 **View transaction history** and download wallet statements
- ⭐ **Save frequent contacts** as beneficiaries for quick transfers
- 🔔 **Get notified** automatically when a transaction happens

**What makes this a real engineering project (not a tutorial)?**

| Challenge | How PayFlow Solves It |
|-----------|----------------------|
| Two people send money from same wallet at once | **Pessimistic Locking** — database locks the wallet row, processes one at a time |
| Network glitch causes same payment request twice | **Idempotency Keys** — duplicate detected, money deducted only once |
| Two transfers between same wallets cause system freeze | **Deadlock Prevention** — wallets always locked in fixed order |
| Sending email slows down the payment | **Event-Driven Architecture** — payment completes instantly, email sent in background via RabbitMQ |
| One service goes down, others break | **Microservices** — each service runs independently with its own database |

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         API Clients                             │
│                   (Mobile App / Web App)                        │
└───────────┬──────────────────┬──────────────────┬───────────────┘
            │                  │                  │
            ▼                  ▼                  ▼
  ┌─────────────────┐ ┌──────────────────┐ ┌──────────────────┐
  │  🔐 Auth        │ │  💰 Wallet       │ │  🔔 Notification │
  │  Service        │ │  Service         │ │  Service         │
  │  (:8081)        │ │  (:8082)         │ │  (:8083)         │
  │                 │ │                  │ │                  │
  │ • Register      │ │ • Create Wallet  │ │ • Email Alerts   │
  │ • Login (JWT)   │ │ • Add Money      │ │ • Templates      │
  │ • User Profile  │ │ • Send Money     │ │ • Retry Failed   │
  │ • Role Access   │ │ • Statement      │ │ • Statistics     │
  │                 │ │ • Beneficiaries  │ │                  │
  └────────┬────────┘ └───────┬──┬───────┘ └────────▲─────────┘
           │                  │  │                   │
           │                  │  │   ┌───────────┐   │
           │                  │  └──►│ 🐰        │───┘
           │                  │      │ RabbitMQ   │
           │                  │      │ (Messages) │
           │                  │      └───────────┘
  ┌────────▼────────┐ ┌──────▼─────────┐ ┌──────────────────┐
  │  🗄️ payflow_   │ │  🗄️ payflow_  │ │  🗄️ payflow_    │
  │  auth (MySQL)   │ │  wallet (MySQL) │ │  notification    │
  │                 │ │                 │ │  (MySQL)         │
  └─────────────────┘ └────────────────┘ └──────────────────┘
```

> Each service has its **own database** — no shared tables, no tight coupling. Services communicate through **REST APIs** (synchronous) and **RabbitMQ** (asynchronous events).

---

## 🛠 Tech Stack

<table>
<tr>
<td align="center" width="96">
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/java/java-original.svg" width="48" height="48" alt="Java" />
<br><b>Java 17</b>
</td>
<td align="center" width="96">
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/spring/spring-original.svg" width="48" height="48" alt="Spring" />
<br><b>Spring Boot 3</b>
</td>
<td align="center" width="96">
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/mysql/mysql-original.svg" width="48" height="48" alt="MySQL" />
<br><b>MySQL 8</b>
</td>
<td align="center" width="96">
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/docker/docker-original.svg" width="48" height="48" alt="Docker" />
<br><b>Docker</b>
</td>
<td align="center" width="96">
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/rabbitmq/rabbitmq-original.svg" width="48" height="48" alt="RabbitMQ" />
<br><b>RabbitMQ</b>
</td>
</tr>
</table>

| Category | Technologies |
|:---------|:------------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.2, Spring Security, Spring Data JPA |
| **Authentication** | JWT (JSON Web Tokens), BCrypt password hashing |
| **ORM & Database** | Hibernate, MySQL 8 (one database per service) |
| **Messaging** | RabbitMQ (AMQP) for async event-driven communication |
| **API Docs** | Swagger / OpenAPI 3 (interactive API playground) |
| **Testing** | JUnit 5, Mockito, H2 in-memory DB (60+ test cases) |
| **Containerization** | Docker, Docker Compose (one-command startup) |
| **CI/CD** | GitHub Actions (automated build + test on every push) |

---

## ✨ Features

### 🔐 Auth Service — User Identity & Security
| Feature | Description |
|---------|-------------|
| User Registration | Sign up with email, username, password (validated) |
| JWT Login | Login returns a signed token — no session storage needed |
| Password Security | BCrypt hashing — passwords never stored in plain text |
| Role-Based Access | ADMIN and USER roles with different permissions |
| Protected Routes | Profile endpoint only accessible with valid JWT |

### 💰 Wallet Service — Core Payment Engine
| Feature | Description |
|---------|-------------|
| Wallet Management | Create, view, freeze, and unfreeze wallets |
| Add Money | Top-up wallet balance (like adding money to Paytm) |
| Send Money | Transfer to another wallet with real-time balance update |
| **Pessimistic Locking** | Database-level locks prevent double-spending |
| **Idempotent Transactions** | Duplicate requests safely return same result |
| **Deadlock Prevention** | Wallets locked in ascending ID order |
| Daily Transfer Limits | Configurable limit (default: ₹1,00,000/day) |
| Transaction Reversal | Reverse completed transactions (compensating transaction) |
| Beneficiary Management | Save, list, and remove frequent transfer recipients |
| Wallet Statements | Date-filtered history with total credits/debits summary |
| BigDecimal Precision | All money calculations use BigDecimal (no rounding errors) |

### 🔔 Notification Service — Smart Alerts
| Feature | Description |
|---------|-------------|
| Event-Driven | Listens to RabbitMQ — gets triggered automatically on transactions |
| Email Notifications | Sends transaction receipts and alerts |
| Templates | Create reusable notification templates with {{variables}} |
| Retry Mechanism | Failed notifications auto-retry (configurable max retries) |
| Statistics Dashboard | Track sent, pending, and failed notification counts |

---

## 📡 API Endpoints

### 🔐 Authentication (`auth-service` — port 8081)

| Method | Endpoint | Description |
|:------:|----------|-------------|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Login and receive JWT token |
| `GET` | `/api/auth/profile` | Get current user profile (requires JWT) |

### 💰 Wallets (`wallet-service` — port 8082)

| Method | Endpoint | Description |
|:------:|----------|-------------|
| `POST` | `/api/wallets` | Create a new wallet |
| `GET` | `/api/wallets/{walletId}` | Get wallet details |
| `GET` | `/api/wallets/user/{userId}` | Get wallet by user ID |
| `PUT` | `/api/wallets/{walletId}/freeze` | Freeze a wallet |
| `PUT` | `/api/wallets/{walletId}/unfreeze` | Unfreeze a wallet |

### 💸 Transactions (`wallet-service` — port 8082)

| Method | Endpoint | Description |
|:------:|----------|-------------|
| `POST` | `/api/transactions/wallets/{walletId}/add-money` | Add money to wallet |
| `POST` | `/api/transactions/wallets/{walletId}/send-money` | Send money to another wallet |
| `GET` | `/api/transactions/{transactionId}` | Get transaction details |
| `GET` | `/api/transactions/wallets/{walletId}/history` | Transaction history (paginated) |
| `GET` | `/api/transactions/wallets/{walletId}/statement` | Wallet statement (date range) |
| `POST` | `/api/transactions/{transactionId}/reverse` | Reverse a transaction |

### ⭐ Beneficiaries (`wallet-service` — port 8082)

| Method | Endpoint | Description |
|:------:|----------|-------------|
| `POST` | `/api/wallets/{walletId}/beneficiaries` | Add a beneficiary |
| `GET` | `/api/wallets/{walletId}/beneficiaries` | List all beneficiaries |
| `DELETE` | `/api/wallets/{walletId}/beneficiaries/{id}` | Remove a beneficiary |

### 🔔 Notifications (`notification-service` — port 8083)

| Method | Endpoint | Description |
|:------:|----------|-------------|
| `GET` | `/api/notifications` | List all notifications |
| `GET` | `/api/notifications/{id}` | Get notification details |
| `POST` | `/api/notifications/{id}/retry` | Retry a failed notification |
| `GET` | `/api/notifications/stats` | Notification statistics |
| `POST` | `/api/notification-templates` | Create notification template |
| `GET` | `/api/notification-templates` | List all templates |
| `PUT` | `/api/notification-templates/{id}` | Update a template |
| `DELETE` | `/api/notification-templates/{id}` | Delete a template |

---

## 🚀 Getting Started

### Prerequisites

- ☕ Java 17+
- 📦 Maven 3.8+
- 🐳 Docker & Docker Compose

### Quick Start (Docker) — One Command Setup

```bash
git clone https://github.com/Shubh2-0/PayFlow.git
cd PayFlow
docker-compose up --build
```

That's it! All services will be running:

| Service | URL | Swagger Docs |
|---------|-----|:------------:|
| 🔐 Auth Service | http://localhost:8081 | [Open](http://localhost:8081/swagger-ui.html) |
| 💰 Wallet Service | http://localhost:8082 | [Open](http://localhost:8082/swagger-ui.html) |
| 🔔 Notification Service | http://localhost:8083 | [Open](http://localhost:8083/swagger-ui.html) |
| 🐰 RabbitMQ Dashboard | http://localhost:15672 | — |

### Manual Setup (without Docker)

1. Start MySQL and create databases:
   ```sql
   CREATE DATABASE payflow_auth;
   CREATE DATABASE payflow_wallet;
   CREATE DATABASE payflow_notification;
   ```

2. Start RabbitMQ on port 5672

3. Run each service:
   ```bash
   cd auth-service && mvn spring-boot:run
   cd wallet-service && mvn spring-boot:run
   cd notification-service && mvn spring-boot:run
   ```

---

## 📘 Usage Examples

**1. Register a user:**
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "email": "john@example.com",
    "password": "secret123",
    "fullName": "John Doe"
  }'
```

**2. Login and get JWT token:**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "password": "secret123"}'
```

**3. Add ₹5,000 to wallet:**
```bash
curl -X POST http://localhost:8082/api/transactions/wallets/1/add-money \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5000.00,
    "description": "Initial top-up",
    "idempotencyKey": "topup-001"
  }'
```

**4. Send ₹1,500 to another wallet:**
```bash
curl -X POST http://localhost:8082/api/transactions/wallets/1/send-money \
  -H "Content-Type: application/json" \
  -d '{
    "receiverWalletId": 2,
    "amount": 1500.00,
    "description": "Payment for services",
    "idempotencyKey": "txn-001"
  }'
```

---

## 📁 Project Structure

```
PayFlow/
│
├── 🐳 docker-compose.yml          # One-command setup for all services
├── 🗄️ init-db.sql                 # Auto-creates all 3 databases
├── 📦 pom.xml                      # Parent Maven POM
│
├── 🔐 auth-service/                # Authentication & User Management
│   └── src/main/java/
│       ├── controller/             # REST API endpoints
│       ├── dto/                    # Request/Response objects
│       ├── entity/                 # User, Role (JPA entities)
│       ├── exception/              # Error handling
│       ├── repository/             # Database queries
│       ├── security/               # JWT filter, config, token service
│       └── service/                # Business logic
│
├── 💰 wallet-service/              # Core Payment Engine
│   └── src/main/java/
│       ├── controller/             # Wallet, Transaction, Beneficiary APIs
│       ├── dto/                    # 10 DTOs for all operations
│       ├── entity/                 # Wallet, Transaction, Beneficiary
│       ├── enums/                  # WalletStatus, TransactionType/Status
│       ├── event/                  # RabbitMQ event publisher
│       ├── exception/              # 6 domain-specific exceptions
│       ├── repository/             # Queries with pessimistic locking
│       └── service/                # Transaction logic, daily limits
│
├── 🔔 notification-service/        # Event-Driven Notifications
│   └── src/main/java/
│       ├── config/                 # RabbitMQ queue/exchange setup
│       ├── controller/             # Notification & Template APIs
│       ├── dto/                    # TransactionEvent, responses
│       ├── entity/                 # Notification, Template
│       ├── listener/               # RabbitMQ event consumer
│       ├── repository/             # Database queries
│       └── service/                # Email & notification logic
│
└── 🔄 .github/workflows/
    └── ci.yml                      # GitHub Actions CI pipeline
```

---

## 🧠 Design Decisions

> These are the engineering choices that make PayFlow production-grade, not just another CRUD project.

### 1. Why Pessimistic Locking for Balance Updates?

**Problem:** If two requests read wallet balance (₹1000) at the same time, both deduct ₹500, and both write ₹500 — the user loses ₹500.

**Solution:** `SELECT ... FOR UPDATE` locks the wallet row. Second request waits until first completes. No money is lost.

**Why not Optimistic Locking?** In payments, retrying a failed transaction is risky and expensive. Better to wait 10ms for a lock than risk incorrect balances.

### 2. Why Idempotency Keys?

**Problem:** User clicks "Pay" → network timeout → user clicks again → money deducted twice.

**Solution:** Every transaction has a unique `idempotencyKey`. If the same key comes again, we return the existing result instead of creating a new transaction. This is how Stripe, Razorpay, and every production payment API works.

### 3. Why Ordered Locking for Deadlock Prevention?

**Problem:** Transfer A→B locks wallet A, then tries to lock B. Simultaneously, transfer B→A locks wallet B, then tries to lock A. Both wait forever = **deadlock**.

**Solution:** Always lock the wallet with the **smaller ID first**. Both transfers lock A first, then B. No circular wait = no deadlock. Ever.

### 4. Why Event-Driven Notifications?

**Problem:** Sending email inside the payment transaction — if email server is slow (3 seconds), payment is slow. If email fails, does the payment rollback?

**Solution:** Payment completes → event published to RabbitMQ → notification service picks it up independently. Payment is fast, email failures don't affect payments, and we can add new consumers (SMS, push notifications) without changing wallet-service.

### 5. Why Database Per Service?

**Problem:** Shared database = one service changes a table, other service breaks. Tight coupling defeats the purpose of microservices.

**Solution:** `payflow_auth`, `payflow_wallet`, `payflow_notification` — three separate databases. Each service owns its data. No cross-database joins.

---

## 🧪 Testing

**60+ test cases** across **12 test classes** in all three services:

| Type | What it Tests | Tools |
|------|--------------|-------|
| **Unit Tests** | Service layer logic in isolation | JUnit 5, Mockito |
| **Controller Tests** | API endpoints, request validation, error responses | @WebMvcTest, MockMvc |
| **Repository Tests** | Database queries and JPA mappings | @DataJpaTest, H2 |

```bash
# Run all tests
cd auth-service && mvn test
cd wallet-service && mvn test
cd notification-service && mvn test
```

---

## 🔄 CI/CD

GitHub Actions automatically runs on every push:

1. ☕ Sets up JDK 17 (Temurin) with Maven caching
2. 🔨 Builds all three services (`mvn clean verify`)
3. 🧪 Runs complete test suite

---

## 📄 License

This project is licensed under the MIT License.

---

<div align="center">

**Built with ❤️ by [Shubham Bhati](https://github.com/Shubh2-0)**

⭐ Star this repo if you found it useful!

</div>
