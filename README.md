# PayFlow — Digital Wallet & Payment System

A production-grade **digital wallet and payment backend** built with **microservices architecture** using Java 17 and Spring Boot 3. Designed to handle real-world challenges of financial systems — concurrent balance updates, duplicate transaction prevention, and reliable notification delivery.

**What makes this different from a typical CRUD project:**

- **Pessimistic Locking** — Prevents double-spending when multiple transactions hit the same wallet simultaneously
- **Idempotent Transactions** — Safe against network retries; same request twice = single debit, not two
- **Deadlock Prevention** — Wallets always locked in ascending ID order during transfers, making deadlocks structurally impossible
- **Event-Driven Architecture** — Transaction events flow through RabbitMQ to decouple services; notification failures never block payments
- **60+ Test Cases** — Unit tests (Mockito), controller tests (MockMvc), repository tests (H2) across all services

---

## Architecture

```
                          +-------------------+
                          |    API Clients    |
                          +--------+----------+
                                   |
                 +-----------------+-----------------+
                 |                 |                 |
                 v                 v                 v
          +------+------+  +------+------+  +-------+--------+
          | auth-service|  |wallet-service|  |notification-   |
          |   :8081     |  |   :8082     |  |service :8083   |
          +------+------+  +------+------+  +-------+--------+
                 |                |  |               ^
                 |                |  |               |
                 |                |  +-----+---------+
                 |                |        |
                 |                |   +----+-----+
                 |                |   | RabbitMQ |
                 |                |   |  :5672   |
                 |                |   +----------+
                 |                |
          +------+------+  +-----+-------+  +---------------+
          |payflow_auth |  |payflow_wallet|  |payflow_notif. |
          |  (MySQL)    |  |  (MySQL)     |  |  (MySQL)      |
          +-------------+  +-------------+  +---------------+
```

Each service owns its database. The wallet-service publishes transaction events to RabbitMQ, which the notification-service consumes asynchronously to send email alerts.

---

## Tech Stack

| Layer              | Technology                          |
|--------------------|-------------------------------------|
| Language           | Java 17                             |
| Framework          | Spring Boot 3.2                     |
| Security           | Spring Security, JWT, BCrypt        |
| Persistence        | Spring Data JPA, Hibernate, MySQL 8 |
| Messaging          | RabbitMQ (AMQP)                     |
| API Documentation  | Swagger / OpenAPI 3                 |
| Testing            | JUnit 5, Mockito, H2 (in-memory)   |
| Containerization   | Docker, Docker Compose              |
| CI/CD              | GitHub Actions                      |

---

## Features

### auth-service
- User registration with input validation
- JWT-based authentication (login returns signed token)
- Password hashing with BCrypt
- Role-based access control (USER / ADMIN)
- Protected profile endpoint

### wallet-service
- Wallet creation, lookup, freeze/unfreeze
- **Pessimistic locking** on balance mutations (no double-spend)
- **Idempotent transactions** via unique idempotency keys (safe retries)
- **Deadlock prevention** -- wallets locked in ascending ID order during transfers
- **Daily transfer limits** (configurable, default 100,000 INR)
- Peer-to-peer money transfers with full ACID guarantees
- Transaction reversal (compensating transactions)
- Beneficiary management (save, list, remove frequent recipients)
- Wallet statements with date-range filtering and aggregated totals
- All monetary values use `BigDecimal` (no floating-point drift)

### notification-service
- Event-driven architecture -- consumes transaction events from RabbitMQ
- Email notifications via Spring Mail
- Notification templates (CRUD) with variable substitution
- Retry support for failed notifications
- Notification statistics dashboard

---

## API Endpoints

### Authentication (auth-service -- :8081)

| Method | Endpoint              | Description                     |
|--------|-----------------------|---------------------------------|
| POST   | `/api/auth/register`  | Register a new user             |
| POST   | `/api/auth/login`     | Authenticate and receive JWT    |
| GET    | `/api/auth/profile`   | Get current user profile        |

### Wallets (wallet-service -- :8082)

| Method | Endpoint                       | Description                  |
|--------|--------------------------------|------------------------------|
| POST   | `/api/wallets`                 | Create a new wallet          |
| GET    | `/api/wallets/{walletId}`      | Get wallet by ID             |
| GET    | `/api/wallets/user/{userId}`   | Get wallet by user ID        |
| PUT    | `/api/wallets/{walletId}/freeze`   | Freeze a wallet          |
| PUT    | `/api/wallets/{walletId}/unfreeze` | Unfreeze a wallet        |

### Transactions (wallet-service -- :8082)

| Method | Endpoint                                        | Description                       |
|--------|--------------------------------------------------|-----------------------------------|
| POST   | `/api/transactions/wallets/{walletId}/add-money`  | Add money to wallet (top-up)     |
| POST   | `/api/transactions/wallets/{walletId}/send-money` | Transfer money to another wallet |
| GET    | `/api/transactions/{transactionId}`               | Get transaction details          |
| GET    | `/api/transactions/wallets/{walletId}/history`    | Paginated transaction history    |
| GET    | `/api/transactions/wallets/{walletId}/statement`  | Statement with date range filter |
| POST   | `/api/transactions/{transactionId}/reverse`       | Reverse a transaction            |

### Beneficiaries (wallet-service -- :8082)

| Method | Endpoint                                                | Description            |
|--------|---------------------------------------------------------|------------------------|
| POST   | `/api/wallets/{walletId}/beneficiaries`                 | Add a beneficiary      |
| GET    | `/api/wallets/{walletId}/beneficiaries`                 | List beneficiaries     |
| DELETE | `/api/wallets/{walletId}/beneficiaries/{beneficiaryId}` | Remove a beneficiary   |

### Notifications (notification-service -- :8083)

| Method | Endpoint                              | Description                    |
|--------|---------------------------------------|--------------------------------|
| GET    | `/api/notifications`                  | List notifications (paginated) |
| GET    | `/api/notifications/{id}`             | Get notification by ID         |
| POST   | `/api/notifications/{id}/retry`       | Retry a failed notification    |
| GET    | `/api/notifications/stats`            | Notification statistics        |
| POST   | `/api/notification-templates`         | Create a template              |
| GET    | `/api/notification-templates`         | List active templates          |
| PUT    | `/api/notification-templates/{id}`    | Update a template              |
| DELETE | `/api/notification-templates/{id}`    | Delete a template              |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### Quick Start (Docker)

```bash
git clone https://github.com/Shubh2-0/PayFlow.git
cd PayFlow
docker-compose up --build
```

This starts MySQL, RabbitMQ, and all three services. Wait for health checks to pass (~30 seconds).

| Service              | URL                              |
|----------------------|----------------------------------|
| auth-service         | http://localhost:8081             |
| wallet-service       | http://localhost:8082             |
| notification-service | http://localhost:8083             |
| RabbitMQ Management  | http://localhost:15672            |

### Manual Setup (without Docker)

1. Start MySQL on port 3306 and create the databases:
   ```sql
   CREATE DATABASE payflow_auth;
   CREATE DATABASE payflow_wallet;
   CREATE DATABASE payflow_notification;
   ```

2. Start RabbitMQ on port 5672.

3. Build and run each service:
   ```bash
   cd auth-service && mvn spring-boot:run
   cd wallet-service && mvn spring-boot:run
   cd notification-service && mvn spring-boot:run
   ```

### API Documentation

Each service exposes Swagger UI:

- http://localhost:8081/swagger-ui.html
- http://localhost:8082/swagger-ui.html
- http://localhost:8083/swagger-ui.html

---

## Usage Examples

**Register a user:**
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

**Login:**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "password": "secret123"}'
```

**Add money to a wallet:**
```bash
curl -X POST http://localhost:8082/api/transactions/wallets/1/add-money \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5000.00,
    "description": "Initial top-up",
    "idempotencyKey": "topup-001"
  }'
```

**Send money to another wallet:**
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

## Project Structure

```
payflow/
├── docker-compose.yml
├── init-db.sql
├── pom.xml
│
├── auth-service/
│   ├── Dockerfile
│   └── src/main/java/com/flowforge/auth/
│       ├── controller/        # REST endpoints
│       ├── dto/               # Request/response objects
│       ├── entity/            # JPA entities (User, Role)
│       ├── exception/         # Global exception handling
│       ├── repository/        # Spring Data repositories
│       ├── security/          # JWT filter, config, service
│       └── service/           # Business logic
│
├── wallet-service/
│   ├── Dockerfile
│   └── src/main/java/com/payflow/wallet/
│       ├── controller/        # Wallet, Transaction, Beneficiary APIs
│       ├── dto/               # Request/response objects
│       ├── entity/            # Wallet, Transaction, BeneficiaryWallet
│       ├── enums/             # TransactionType, TransactionStatus, WalletStatus
│       ├── event/             # RabbitMQ event publisher
│       ├── exception/         # Domain-specific exceptions
│       ├── repository/        # Spring Data repositories
│       └── service/           # Core business logic + daily limits
│
├── notification-service/
│   ├── Dockerfile
│   └── src/main/java/com/flowforge/notification/
│       ├── config/            # RabbitMQ configuration
│       ├── controller/        # Notification + template APIs
│       ├── dto/               # DTOs including TransactionEvent
│       ├── entity/            # Notification, NotificationTemplate
│       ├── listener/          # RabbitMQ event consumer
│       ├── repository/        # Spring Data repositories
│       └── service/           # Email + notification logic
│
└── .github/workflows/
    └── ci.yml                 # GitHub Actions pipeline
```

---

## Design Decisions

### 1. Pessimistic Locking for Balance Updates

Wallet balances are updated under `PESSIMISTIC_WRITE` locks acquired via `SELECT ... FOR UPDATE`. This prevents two concurrent transactions from reading the same balance and both writing stale results. Optimistic locking with version columns was considered but rejected -- in a payment system, retrying failed transactions is more expensive than holding a short lock.

### 2. Idempotency Keys

Every transaction requires a client-supplied `idempotencyKey`. Before processing, the service checks if a transaction with that key already exists. If it does, the existing result is returned without re-executing. This makes the API safe against network retries, duplicate webhook deliveries, and client-side retry logic -- a requirement for any financial API.

### 3. Deadlock Prevention via Ordered Locking

During a transfer between wallet A and wallet B, both wallets must be locked. If two concurrent transfers lock in opposite order (A then B vs. B then A), a database deadlock occurs. PayFlow always acquires locks in ascending wallet ID order (`Math.min` first, `Math.max` second), making deadlocks structurally impossible.

### 4. Event-Driven Notifications

The wallet-service does not call the notification-service directly. Instead, it publishes a `TransactionEvent` to RabbitMQ after each transaction. The notification-service consumes these events independently. This means:
- A notification failure never blocks or rolls back a transaction
- Services can be deployed and scaled independently
- Adding new event consumers (analytics, audit logging) requires zero changes to wallet-service

### 5. Database Per Service

Each microservice has its own MySQL database (`payflow_auth`, `payflow_wallet`, `payflow_notification`). There are no cross-database joins or shared tables. This enforces loose coupling at the data layer and allows each service to evolve its schema independently.

---

## Testing

The project includes **12 test classes** across all three services, covering:

- **Unit tests** -- Service layer logic tested in isolation with Mockito mocks
- **Controller tests** -- `@WebMvcTest` with MockMvc for endpoint validation, serialization, and error handling
- **Repository tests** -- `@DataJpaTest` with H2 in-memory database for query verification

Run all tests:
```bash
# All services
cd auth-service && mvn test
cd wallet-service && mvn test
cd notification-service && mvn test
```

---

## CI/CD

GitHub Actions runs on every push to `main`/`develop` and on pull requests:

1. Sets up JDK 17 (Temurin) with Maven caching
2. Builds each service (`mvn clean verify`)
3. Runs the full test suite for all three services

See [`.github/workflows/ci.yml`](.github/workflows/ci.yml) for the pipeline definition.

---

## License

MIT
