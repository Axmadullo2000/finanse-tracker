# Web Module

The HTTP layer of FinTrack. Contains REST controllers, security configuration, request/response DTOs, and the application entry point.

## Responsibility

- Parse HTTP requests and return HTTP responses
- Authenticate and authorize requests via Spring Security
- Validate incoming request bodies (`@Valid`)
- Convert between web DTOs and domain types
- Map exceptions to appropriate HTTP status codes
- Host the `@SpringBootApplication` main class

## Application Entry Point

```java
package com.fintrack;

@SpringBootApplication
public class FinTrackApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinTrackApplication.class, args);
    }
}
```

Located at `com.fintrack` so component scan covers all three modules (`com.fintrack.entity`, `com.fintrack.service`, `com.fintrack.controller`, etc.).

## Security Configuration

### SecurityConfig

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    // SecurityFilterChain — defines URL-level access rules
    // BCryptPasswordEncoder — production-grade password hashing
    // AuthenticationManager — exposed for custom login endpoint
}
```

**URL-level security:**

| Pattern | Access |
|---|---|
| `/api/auth/register` | Public (permitAll) |
| `/api/auth/login` | Public (permitAll) |
| `/api/admin/**` | ADMIN role only |
| Everything else | Authenticated |

**Custom entry point:** Unauthenticated API requests (`/api/**`) return `401` with a text message instead of redirecting to an HTML login page.

### CustomUserDetailsService

Bridges Spring Security with the `User` entity:

1. Spring Security receives username/password from login request
2. Calls `loadUserByUsername(username)`
3. Service queries `UserRepository.findByUsername()`
4. Converts `User` entity → Spring's `UserDetails` object
5. Spring compares BCrypt-encoded passwords
6. On success → creates session with `JSESSIONID` cookie

### AuthenticatedUser

Utility component injected into all controllers to get the current username:

```java
@Component
public class AuthenticatedUser {
    public String getUsername() {
        return SecurityContextHolder.getContext()
            .getAuthentication().getName();
    }
}
```

Eliminates the repeated `SecurityContextHolder` boilerplate across controllers.

### Custom Login Endpoint

Unlike default Spring form login, this endpoint accepts **JSON** and creates a session programmatically:

```java
@PostMapping("/login")
public ResponseEntity<String> login(@RequestBody LoginRequest request,
                                    HttpServletRequest httpRequest) {
    // 1. Authenticate via AuthenticationManager
    // 2. Create SecurityContext
    // 3. Store in session
    // 4. Return success response
}
```

This is the pattern for REST APIs where clients send JSON, not form data.

## Controllers

### AuthController (`/api/auth`)

| Method | Path | Description |
|---|---|---|
| POST | `/register` | Register new user |
| POST | `/login` | JSON-based login |
| GET | `/me` | Get current username |

### CategoryController (`/api/categories`)

Full CRUD. Every operation filters by authenticated user — users only see their own categories.

### ExpenseController (`/api/expenses`)

Full CRUD with pagination and filtering. Supports query parameters for category, date range, page, size, and sort.

**Pagination:** Uses Spring Data's `Pageable` parameter, automatically parsed from query params (`?page=0&size=10&sort=date,desc`).

### BudgetController (`/api/budgets`)

Create and update budgets. One budget per category per month per user.

### SummaryController (`/api/summary`)

Read-only. Returns monthly spending vs budget comparison per category.

### AdminController (`/api/admin`)

Protected by `@PreAuthorize("hasRole('ADMIN')")` at class level. List users, delete users, view system stats.

## Request/Response DTOs

### Request DTOs (incoming)

All request DTOs use Bean Validation annotations:

```java
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 50) String username;
    @NotBlank @Size(min = 6) String password;
    @NotBlank @Email String email;
}

public class CategoryRequest {
    @NotBlank @Size(min = 2, max = 50) String name;
}

public class ExpenseRequest {
    @NotNull @Positive BigDecimal amount;
    String description;
    @NotNull LocalDate date;
    @NotNull Long categoryId;
}

public class BudgetRequest {
    @NotNull @Positive BigDecimal monthlyLimit;
    @NotNull @Min(1) @Max(12) Integer month;
    @NotNull @Min(2020) Integer year;
    @NotNull Long categoryId;
}
```

### Response DTOs (outgoing)

Response DTOs expose only what the client needs — never internal fields like password or user references:

```java
public class CategoryResponse { Long id; String name; }
public class ExpenseResponse { Long id; BigDecimal amount; String description;
                               LocalDate date; Long categoryId; String categoryName; }
public class BudgetResponse { Long id; BigDecimal monthlyLimit; Integer month;
                              Integer year; Long categoryId; String categoryName; }
public class MonthlySummaryResponse { Long categoryId; String categoryName;
                                      BigDecimal spent; BigDecimal budgetLimit;
                                      BigDecimal remaining; }
public class UserResponse { Long id; String username; String email; String role; }
```

## Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    NoSuchElementException          → 404 Not Found
    IllegalArgumentException        → 400 Bad Request
    AccessDeniedException           → 403 Forbidden
    MethodArgumentNotValidException → 400 Bad Request (field → message map)
}
```

Validation errors return a map of field names to error messages:

```json
{
    "username": "must not be blank",
    "email": "must be a well-formed email address"
}
```

## Admin Seeding (CommandLineRunner)

On first startup, a `Runner` component creates an admin user if one doesn't exist:

```java
@Component
public class Runner implements CommandLineRunner {
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            // Create admin with BCrypt-encoded password and ADMIN role
        }
    }
}
```

The `if` check prevents duplicate creation on subsequent restarts.

## Configuration

`web/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/fintrack
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

**Why `application.properties` lives here:** Only the web module runs as an application (`@SpringBootApplication`). Spring Boot loads properties from the running module's classpath. Properties in library modules (persistence, service) would be ignored or cause conflicts.

## Testing

Integration tests use `@SpringBootTest` + `@AutoConfigureMockMvc`:

```java
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FinTrackIntegrationTest { ... }
```

- **`@WithMockUser(username = "testuser")`** — Simulates authenticated user without actual login
- **`@WithMockUser(username = "admin", roles = "ADMIN")`** — Simulates admin user
- **`@DirtiesContext(AFTER_EACH_TEST_METHOD)`** — Fresh database for every test
- **`jsonPath("$.field")`** — Asserts JSON response structure

Test `application.properties` uses `spring.jpa.hibernate.ddl-auto=create-drop` for clean state.

### Test Coverage

| Area | Tests |
|---|---|
| Auth | Register success/failure, login, 401 for unauthenticated |
| Category | CRUD, validation, user isolation |
| Expense | CRUD, filtering, pagination, user isolation |
| Budget | Create, duplicate prevention |
| Summary | Monthly spending vs budget |
| Admin | 403 for USER role, 200 for ADMIN role |

## Dependencies

```xml
<!-- Sibling module -->
<dependency>
    <groupId>com.security.app</groupId>
    <artifactId>service</artifactId>
</dependency>

<!-- Spring starters -->
<dependency>spring-boot-starter-web</dependency>
<dependency>spring-boot-starter-security</dependency>
<dependency>spring-boot-starter-validation</dependency>

<!-- Test -->
<dependency>spring-security-test (test scope)</dependency>

<!-- Build plugin -->
<plugin>spring-boot-maven-plugin</plugin>
```

`spring-boot-maven-plugin` is only in this module because web is the only runnable module (produces executable fat JAR).
