# FinTrack — Personal Finance Tracker

A multi-module Spring Boot application for tracking personal expenses, managing budgets, and viewing monthly spending summaries.

## Tech Stack

- **Java 17**
- **Spring Boot 3.5.13**
- **Spring Security** — Session-based authentication, role-based authorization
- **Spring Data JPA / Hibernate** — ORM and database access
- **PostgreSQL** — Production database
- **Maven** — Multi-module build system
- **Lombok** — Boilerplate reduction

## Architecture

The project follows a layered multi-module architecture where each Maven module has a single responsibility.

```
fintrack/
├── persistence/    Data access layer (entities, repositories)
├── service/        Business logic layer (services, DTOs, exceptions)
└── web/            HTTP layer (controllers, security, request/response DTOs)
```

### Dependency Direction

```
web  →  service  →  persistence
```

- `web` depends on `service` (calls business logic)
- `service` depends on `persistence` (accesses database)
- `persistence` depends on nothing internal (pure data layer)

This enforces clean separation: controllers never touch repositories directly, services never know about HTTP, and entities never know about either.

### Package Structure

Every class across all modules lives under `com.fintrack.*` so that a single `@SpringBootApplication` in the web module scans everything automatically.

```
com.fintrack/
├── entity/         Entities (persistence module)
├── repository/     JPA repositories (persistence module)
├── service/        Services, filters, DTOs (service module)
├── controller/     REST controllers (web module)
├── dto/            Request/Response DTOs (web module)
├── security/       Security config, auth utilities (web module)
├── exception/      Global exception handler (web module)
└── runnable/       Startup runners (web module)
```

## Domain Model

```
User ──< Category ──< Expense
              │
              └──< Budget
```

- **User** — Application user with username, email, password (BCrypt), and role (USER/ADMIN)
- **Category** — Spending category (Food, Transport, etc.) owned by a user
- **Expense** — Individual expense with amount, date, description, linked to a category and user
- **Budget** — Monthly spending limit per category per user

### Key Relationships

| Relationship | Type | Owner Side |
|---|---|---|
| User → Category | OneToMany | Category (has `user_id` FK) |
| User → Expense | OneToMany | Expense (has `user_id` FK) |
| User → Budget | OneToMany | Budget (has `user_id` FK) |
| Category → Expense | OneToMany | Expense (has `category_id` FK) |
| Category → Budget | OneToMany | Budget (has `category_id` FK) |

## API Endpoints

### Authentication

| Method | Path | Auth Required | Description |
|---|---|---|---|
| POST | `/api/auth/register` | No | Register new user |
| POST | `/api/auth/login` | No | Login (creates session) |
| GET | `/api/auth/me` | Yes | Get current username |
| POST | `/api/auth/logout` | Yes | Logout (destroys session) |

### Categories

| Method | Path | Auth Required | Description |
|---|---|---|---|
| GET | `/api/categories` | Yes | List user's categories |
| GET | `/api/categories/{id}` | Yes | Get single category |
| POST | `/api/categories` | Yes | Create category |
| PUT | `/api/categories/{id}` | Yes | Update category |
| DELETE | `/api/categories/{id}` | Yes | Delete category |

### Expenses

| Method | Path | Auth Required | Description |
|---|---|---|---|
| GET | `/api/expenses` | Yes | List expenses (paginated, filterable) |
| GET | `/api/expenses/{id}` | Yes | Get single expense |
| POST | `/api/expenses` | Yes | Create expense |
| PUT | `/api/expenses/{id}` | Yes | Update expense |
| DELETE | `/api/expenses/{id}` | Yes | Delete expense |

**Expense query parameters:**
- `categoryId` — Filter by category
- `from` — Start date (ISO format: `2026-04-01`)
- `to` — End date (ISO format: `2026-04-30`)
- `page` — Page number (default: 0)
- `size` — Page size (default: 10)
- `sort` — Sort field and direction (e.g., `date,desc`)

### Budgets

| Method | Path | Auth Required | Description |
|---|---|---|---|
| GET | `/api/budgets` | Yes | List user's budgets |
| POST | `/api/budgets` | Yes | Create budget for category/month |
| PUT | `/api/budgets/{id}` | Yes | Update budget limit |

### Summary

| Method | Path | Auth Required | Description |
|---|---|---|---|
| GET | `/api/summary/monthly?month=4&year=2026` | Yes | Monthly spending vs budget per category |

### Admin (requires ADMIN role)

| Method | Path | Auth Required | Description |
|---|---|---|---|
| GET | `/api/admin/users` | ADMIN | List all users |
| DELETE | `/api/admin/users/{id}` | ADMIN | Delete user |
| GET | `/api/admin/stats` | ADMIN | System-wide statistics |

## Security Model

- **Authentication:** Session-based with `JSESSIONID` cookie
- **Password Storage:** BCrypt encoded (never plain text)
- **Authorization:** Role-based (USER, ADMIN) via `@PreAuthorize`
- **Data Isolation:** Every query filters by authenticated user's username
- **Unauthenticated API requests:** Return 401 (not redirect to login page)
- **Admin seeding:** An admin user is created on first startup via `CommandLineRunner`

## Getting Started

### Prerequisites

- Java 17+
- PostgreSQL 14+
- Maven 3.8+

### Database Setup

```sql
CREATE DATABASE fintrack;
```

### Configuration

Edit `web/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/fintrack
spring.datasource.username=postgres
spring.datasource.password=your_password
```

### Build and Run

```bash
# From project root
mvn clean install -DskipTests

# Run the application
cd web_dev
mvn spring-boot:run
```

### Run Tests

```bash
mvn test -pl web_dev
```

## Error Handling

All errors return consistent JSON responses:

| Exception | HTTP Status | When |
|---|---|---|
| `NoSuchElementException` | 404 | Entity not found |
| `IllegalArgumentException` | 400 | Invalid input or business rule violation |
| `AccessDeniedException` | 403 | Insufficient permissions |
| `MethodArgumentNotValidException` | 400 | Bean validation failure |
| Unauthenticated request | 401 | No valid session |

