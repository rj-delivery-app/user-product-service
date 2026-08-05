# user-product-service

The identity and catalog authority for the Deliver Anything platform. This service owns user accounts, JWT authentication, product catalog, categories, and addresses. It is the only service that issues JWT tokens and publishes user and product events to Kafka.

**Port**: 9091
**Package**: `com.example.userproduct`
**Swagger UI**: http://localhost:9091/swagger-ui.html
**Health**: http://localhost:9091/actuator/health

---

## Table of Contents

1. [Overview](#1-overview)
2. [Tech Stack](#2-tech-stack)
3. [Prerequisites](#3-prerequisites)
4. [Configuration](#4-configuration)
5. [Running Locally](#5-running-locally)
6. [Running Tests](#6-running-tests)
7. [API Documentation](#7-api-documentation)
8. [Kafka Events](#8-kafka-events)
9. [Database Entities](#9-database-entities)
10. [Security](#10-security)
11. [Project Structure](#11-project-structure)
12. [Docker](#12-docker)

---

## 1. Overview

**Responsibilities**:

- User registration and login (JWT issuance via HS256)
- User profile management (CRUD)
- Role-based access control (CUSTOMER, MERCHANT_ADMIN, DELIVERY_RIDER, ADMIN)
- Product catalog management (CRUD + availability toggle)
- Category listing
- Address management (CRUD per user)
- Publishing `user.events` and `product.events` to Kafka for downstream services
- Internal REST endpoints (`/internal/**`) for service-to-service communication

**Downstream dependency**: `order-delivery-service` reads user and product data from this service via Kafka events (primary) and via `/internal/**` REST endpoints (fallback).

---

## 2. Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 17 | Runtime |
| Spring Boot | 3.4.1 | Application framework |
| Spring Security | (managed) | JWT authentication and RBAC |
| Spring Data JPA | (managed) | ORM and repository layer |
| Spring Kafka | (managed) | Kafka producer |
| Spring Boot Actuator | (managed) | Health and metrics |
| SpringDoc OpenAPI | 2.8.8 | Swagger UI and API docs |
| jjwt | 0.12.3 | JWT generation and validation |
| PostgreSQL JDBC | 42.7.7 | Database driver |
| Hypersistence Utils | 3.7.3 | JSON column support |
| Lombok | 1.18.38 | Boilerplate reduction |
| JaCoCo | 0.8.12 | Code coverage |
| Maven | 3.9.x (wrapper included) | Build tool |

---

## 3. Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 17 | Required. Use [Adoptium](https://adoptium.net) if not installed. |
| Maven | 3.9.x | Optional — the `./mvnw` wrapper is bundled and requires no system install. |
| PostgreSQL | 16 | Start via `docker compose -f local/docker-compose.yml up -d` from the project root. |
| Kafka | KRaft (Confluent 7.6.0) | Start via the same local compose command above. |

---

## 4. Configuration

All configuration is in `src/main/resources/application.yml`. Values are injected from environment variables at runtime.

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `SERVER_PORT` | `9091` | HTTP listen port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/delivery_db` | JDBC connection URL |
| `SPRING_DATASOURCE_USERNAME` | `duetto` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `password` | Database password |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Schema management (`update` for dev, `validate` for prod) |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9093` | Kafka broker address (host) or `kafka:9094` (container) |
| `JWT_SECRET` | `404E635266...` | HS256 signing secret — **override in production** |
| `JWT_EXPIRATION` | `86400000` | Token TTL in milliseconds (24 hours) |

---

## 5. Running Locally

### Step 1 — Start infrastructure

From the project root:

```bash
docker compose -f local/docker-compose.yml up -d
```

Wait until both `delivery-db` (PostgreSQL) and `kafka` show `healthy`:

```bash
docker compose -f local/docker-compose.yml ps
```

### Step 2 — Run the service

```bash
cd user-product-service
./mvnw spring-boot:run
```

The service reads its configuration from `application.yml`. For local development with the default infrastructure the defaults work without any overrides.

To set environment variables explicitly (e.g., IntelliJ run configuration):

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/delivery_db
export SPRING_DATASOURCE_USERNAME=duetto
export SPRING_DATASOURCE_PASSWORD=password
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9093
export JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
export JWT_EXPIRATION=86400000
export SERVER_PORT=9091
./mvnw spring-boot:run
```

The service is ready when you see `Started UserProductApplication` in the logs.

**Verify**:

```bash
curl http://localhost:9091/api/health
# → {"status":"UP"}
```

---

## 6. Running Tests

Tests use H2 in-memory database — no running PostgreSQL or Kafka instance required.

```bash
# Run unit tests only
./mvnw test

# Run full verify lifecycle (tests + JaCoCo coverage check)
./mvnw clean verify

# Generate JaCoCo HTML coverage report
./mvnw test
open target/site/jacoco/index.html
```

### Coverage thresholds (enforced by JaCoCo)

| Metric | Minimum |
|--------|---------|
| Line coverage | 80% |
| Branch coverage | 70% |

Excluded from coverage measurement: `**/config/**`, `**/dto/**`, `**/entities/**`, `**/exception/ErrorResponse.*`, `**/events/*Event.java`, `**/utils/CustomId.*`, `**/UserProductApplication.*`

---

## 7. API Documentation

Interactive documentation is available at http://localhost:9091/swagger-ui.html when the service is running.

### Authentication (`/api/auth`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/auth/register` | None | Register a new user |
| `POST` | `/api/auth/login` | None | Login and receive a JWT token |
| `POST` | `/api/auth/logout` | Bearer JWT | Logout (client must discard the token) |

**Register request body**:

```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "secret123",
  "phone": "+1-555-0100",
  "role": "CUSTOMER"
}
```

Valid roles: `CUSTOMER`, `MERCHANT_ADMIN`, `DELIVERY_RIDER`

**Login request body**:

```json
{
  "email": "jane@example.com",
  "password": "secret123"
}
```

**Login / register response** (201 for register, 200 for login):

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Jane Doe",
  "email": "jane@example.com",
  "role": "CUSTOMER"
}
```

**curl example**:

```bash
# Register
curl -s -X POST http://localhost:9091/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane Doe","email":"jane@example.com","password":"secret123","phone":"+1-555-0100","role":"CUSTOMER"}' | jq .

# Login
curl -s -X POST http://localhost:9091/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","password":"secret123"}' | jq .
```

---

### Users (`/api/users`)

All user endpoints require a valid `Authorization: Bearer <token>` header.

| Method | Path | Role | Description |
|--------|------|------|-------------|
| `GET` | `/api/users/profile` | Any authenticated | Get own profile |
| `PUT` | `/api/users/profile` | Any authenticated | Update own profile |
| `GET` | `/api/users/{id}` | MERCHANT_ADMIN | Get any user by ID |
| `GET` | `/api/users?pageNo=0&size=20` | MERCHANT_ADMIN | List all users (paginated) |
| `PATCH` | `/api/users/{id}/activate` | MERCHANT_ADMIN | Reactivate a deactivated user |
| `PATCH` | `/api/users/{id}/deactivate` | MERCHANT_ADMIN | Soft-delete a user (sets `isActive=false`) |

**curl example — get own profile**:

```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."
curl -s http://localhost:9091/api/users/profile \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

### Products (`/api/products`)

Public read endpoints require no authentication. Write operations require `MERCHANT_ADMIN`.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/products` | None | List all products (Spring Pageable) |
| `GET` | `/api/products/{id}` | None | Get product by ID |
| `GET` | `/api/products/merchant/{merchantId}` | None | Products by merchant (paginated) |
| `GET` | `/api/products/category/{category}` | None | Products by category (paginated) |
| `GET` | `/api/products/search?query=...` | None | Full-text search by name or description |
| `POST` | `/api/products` | Bearer JWT (MERCHANT_ADMIN) | Create product |
| `PUT` | `/api/products/{id}` | Bearer JWT (MERCHANT_ADMIN owner) | Update product |
| `DELETE` | `/api/products/{id}` | Bearer JWT (MERCHANT_ADMIN owner) | Delete product → 204 No Content |
| `PATCH` | `/api/products/{id}/availability` | Bearer JWT (MERCHANT_ADMIN owner) | Toggle availability flag |

**Create product request body**:

```json
{
  "name": "Margherita Pizza",
  "description": "Classic tomato and mozzarella",
  "price": 12.99,
  "category": "FOOD",
  "imageUrl": "https://example.com/pizza.jpg",
  "isAvailable": true
}
```

**curl example — create product**:

```bash
curl -s -X POST http://localhost:9091/api/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Margherita Pizza","price":12.99,"category":"FOOD","isAvailable":true}' | jq .
```

---

### Categories (`/api/categories`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/categories` | Bearer JWT | List all distinct product category strings |

---

### Addresses (`/api/users/addresses`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/users/addresses` | Bearer JWT | List own addresses |
| `GET` | `/api/users/addresses/{id}` | Bearer JWT | Get specific address (owner only) |
| `POST` | `/api/users/addresses` | Bearer JWT | Add new address |
| `PUT` | `/api/users/addresses/{id}` | Bearer JWT | Update address (owner only) |
| `DELETE` | `/api/users/addresses/{id}` | Bearer JWT | Delete address → 204 No Content |

**Address request body**:

```json
{
  "addressType": "HOME",
  "street": "123 Main St",
  "city": "Springfield",
  "state": "IL",
  "zipCode": "62701"
}
```

Valid `addressType` values: `HOME`, `WORK`, `OTHER`

---

### Health

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/health` | None | Lightweight custom health check |
| `GET` | `/actuator/health` | None | Spring Boot Actuator health (full details) |

```bash
curl http://localhost:9091/api/health
# → {"status":"UP"}

curl http://localhost:9091/actuator/health
# → {"status":"UP","components":{"db":{"status":"UP"},"kafka":{"status":"UP"},...}}
```

---

### Internal Service-to-Service (`/internal/**`)

These endpoints **bypass JWT authentication** and are used exclusively by `order-delivery-service`. They are not routed through nginx and should not be reachable from the public internet.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/internal/users/{id}` | Fetch user by ID |
| `GET` | `/internal/users/{userId}/addresses/{addressId}` | Fetch a specific address for a user |
| `GET` | `/internal/products/{id}` | Fetch product by ID |
| `GET` | `/internal/products/batch?ids=id1,id2,...` | Batch fetch multiple products by IDs |

---

## 8. Kafka Events

This service acts as a **Kafka producer** only. It never consumes any topics.

### Broker configuration

| Context | Address |
|---------|---------|
| Local development (host) | `localhost:9093` |
| Docker Compose (container) | `kafka:9094` |

### Event envelope

All events use this JSON wrapper:

```json
{
  "eventId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "eventType": "USER_CREATED",
  "timestamp": "2025-07-22T10:30:00.000Z",
  "payload": { ... }
}
```

### Topics produced

| Topic | Partitions | Event Types | Kafka Key | Trigger |
|-------|-----------|------------|-----------|---------|
| `user.events` | 3 | `USER_CREATED`, `USER_UPDATED` | `userId` | User registration or profile update |
| `product.events` | 3 | `PRODUCT_CREATED`, `PRODUCT_UPDATED`, `PRODUCT_DELETED` | `productId` | Any product create/update/delete |

### `USER_CREATED` payload

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Jane Doe",
  "email": "jane@example.com",
  "phone": "+1-555-0100",
  "role": "CUSTOMER",
  "isActive": true
}
```

### `PRODUCT_CREATED` / `PRODUCT_UPDATED` payload

```json
{
  "productId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "merchantId": "550e8400-e29b-41d4-a716-446655440001",
  "name": "Margherita Pizza",
  "price": 12.99,
  "category": "FOOD",
  "isAvailable": true
}
```

### `PRODUCT_DELETED` payload

Same schema as `PRODUCT_UPDATED`, with `isAvailable: false`.

---

## 9. Database Entities

The service owns a single PostgreSQL database (`user_product_db`). Hibernate manages the schema via `spring.jpa.hibernate.ddl-auto=update`.

### `users`

| Column | Type | Notes |
|--------|------|-------|
| `id` | `VARCHAR` (UUID) | PK, auto-generated |
| `name` | `VARCHAR` | NOT NULL |
| `email` | `VARCHAR` | NOT NULL, UNIQUE — login identifier |
| `password` | `VARCHAR` | NOT NULL, BCrypt hashed |
| `phone` | `VARCHAR` | NOT NULL |
| `role` | `VARCHAR` | `CUSTOMER`, `MERCHANT_ADMIN`, `DELIVERY_RIDER`, `ADMIN` |
| `gender` | `VARCHAR` | nullable |
| `age` | `INTEGER` | nullable |
| `is_active` | `BOOLEAN` | default `true` — soft-delete flag |
| `created_at` | `TIMESTAMP` | auto-set on insert, immutable |
| `updated_at` | `TIMESTAMP` | auto-updated on every write |

### `products`

| Column | Type | Notes |
|--------|------|-------|
| `id` | `VARCHAR` (UUID) | PK |
| `merchant_id` | `VARCHAR` | FK → `users.id` (owner merchant) |
| `name` | `VARCHAR` | NOT NULL |
| `description` | `TEXT` | nullable |
| `price` | `NUMERIC(10,2)` | NOT NULL |
| `image_url` | `VARCHAR` | nullable |
| `category` | `VARCHAR` | NOT NULL |
| `is_available` | `BOOLEAN` | default `true` |
| `created_at` | `TIMESTAMP` | auto-set on insert, immutable |
| `updated_at` | `TIMESTAMP` | auto-updated |

### `addresses`

| Column | Type | Notes |
|--------|------|-------|
| `id` | `VARCHAR` (UUID) | PK |
| `user_id` | `VARCHAR` | FK → `users.id` |
| `address_type` | `VARCHAR` | `HOME`, `WORK`, `OTHER` |
| `street` | `VARCHAR` | nullable |
| `city` | `VARCHAR` | nullable |
| `state` | `VARCHAR` | nullable |
| `zip_code` | `VARCHAR` | nullable |

---

## 10. Security

### JWT Authentication

Tokens are issued at login and must be sent in the `Authorization: Bearer <token>` header on all protected endpoints.

**Token claims**:

| Claim | Value | Purpose |
|-------|-------|---------|
| `sub` | userId (UUID) | Identifies the acting user |
| `role` | User role string | RBAC decisions |
| `name` | Display name | Denormalized into order snapshots |
| `iat` / `exp` | Unix timestamps | Issued-at / expiry (default: 24 hours) |

**Algorithm**: HS256 using `JWT_SECRET`. The same secret is shared with `order-delivery-service` — both services validate tokens independently.

### Role-Based Access Control

| Role | Capabilities |
|------|-------------|
| `CUSTOMER` | Manage own profile and addresses, browse products and categories |
| `MERCHANT_ADMIN` | All CUSTOMER capabilities + manage products, view all users, activate/deactivate users |
| `DELIVERY_RIDER` | Manage own profile and addresses |
| `ADMIN` | Same access as MERCHANT_ADMIN |

### Internal API Security

Endpoints under `/internal/**` are excluded from Spring Security's filter chain. Access control relies on network isolation — these endpoints should only be reachable within the `delivery-net` Docker bridge network, not from the public internet or through nginx.

---

## 11. Project Structure

```
user-product-service/
├── mvnw                        # Maven wrapper
├── pom.xml                     # Dependencies and build config
└── src/
    ├── main/
    │   ├── resources/
    │   │   └── application.yml # Port, datasource, Kafka, JWT config
    │   └── java/com/example/userproduct/
    │       ├── UserProductApplication.java
    │       ├── config/
    │       │   ├── SecurityConfig.java      # Spring Security + JWT filter chain
    │       │   └── KafkaConfig.java         # Kafka producer configuration
    │       ├── controller/
    │       │   ├── AuthController.java      # POST /api/auth/register, login, logout
    │       │   ├── UserController.java      # GET/PUT /api/users/profile, admin CRUD
    │       │   ├── ProductController.java   # Full CRUD /api/products
    │       │   ├── CategoryController.java  # GET /api/categories
    │       │   ├── AddressController.java   # CRUD /api/users/addresses
    │       │   ├── HealthController.java    # GET /api/health
    │       │   └── InternalController.java  # /internal/** (no auth)
    │       ├── service/
    │       │   ├── AuthService.java         # Registration, login, token issuance
    │       │   ├── UserService.java         # User CRUD and role management
    │       │   ├── ProductService.java      # Product CRUD and search
    │       │   ├── AddressService.java      # Address CRUD
    │       │   └── UserContextService.java  # Extract user identity from JWT
    │       ├── entities/
    │       │   ├── User.java
    │       │   ├── UserRole.java            # CUSTOMER, MERCHANT_ADMIN, DELIVERY_RIDER, ADMIN
    │       │   ├── Product.java
    │       │   ├── Address.java
    │       │   └── AddressType.java         # HOME, WORK, OTHER
    │       ├── dao/                         # Spring Data JPA repositories
    │       ├── dto/                         # Request/response DTOs
    │       ├── events/
    │       │   ├── EventPublisher.java      # Wraps and sends Kafka events
    │       │   ├── UserEvent.java           # User event payload DTO
    │       │   └── ProductEvent.java        # Product event payload DTO
    │       ├── security/
    │       │   ├── JwtUtil.java             # Token generation and validation
    │       │   ├── JwtAuthenticationFilter.java
    │       │   ├── CustomUserDetailsService.java
    │       │   └── SecurityConfig.java
    │       ├── exception/                   # Global exception handling (@ControllerAdvice)
    │       └── utils/
    │           └── CustomId.java            # UUID generation helper
    └── test/
        └── java/com/example/userproduct/   # JUnit 5 + Mockito + MockMvc tests
```

---

## 12. Docker

The service ships with a multi-stage Dockerfile in the project root (`user-product-service/Dockerfile`). The build is also managed by `devops/docker/user-product-service/Dockerfile` used by the full-stack Docker Compose.

**Build the image** (from the project root):

```bash
docker build -t user-product-service:latest ./user-product-service/
```

**Run standalone** (requires a reachable PostgreSQL and Kafka):

```bash
docker run -p 9091:9091 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/delivery_db \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9093 \
  -e JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970 \
  user-product-service:latest
```

**Run as part of the full stack**:

```bash
# From the project root
docker compose up -d
```

See the root [README.md](../README.md) for full Docker Compose documentation.
