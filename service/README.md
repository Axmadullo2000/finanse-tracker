# Service Module

The business logic layer of FinTrack. Contains services, DTOs used at the service boundary, and business validation rules.

## Responsibility

- Implement business rules and validation
- Coordinate between repositories
- Manage transactions via `@Transactional`
- Throw meaningful exceptions for business rule violations
- No HTTP awareness — services accept and return domain types or service DTOs

## Services

### UserService

Handles user registration.

```java
User register(String username, String password, String email)
```

**Business rules:**
- Username must be unique → `IllegalArgumentException` if taken
- Email must be unique → `IllegalArgumentException` if taken
- Password is encoded with `BCryptPasswordEncoder` before storage
- New users get `Role.USER` by default

**Why password encoding happens here, not in the controller:**
Encoding is a business rule ("passwords must be stored securely"), not an HTTP concern. If you added a CLI admin tool that creates users, it would call `UserService.register()` and get encoding for free — no duplication.

### CategoryService

CRUD operations for spending categories.

```java
List<Category> getCategories(String username)
Category getCategoryById(Long id, String username)
Category createCategory(String name, String username)
Category updateCategory(Long id, String name, String username)
void deleteCategory(Long id, String username)
```

**Business rules:**
- Each user has their own categories (user-scoped)
- Category names must be unique per user → `IllegalArgumentException` if duplicate
- Users can only access their own categories → `NoSuchElementException` if not found (query includes username)

### ExpenseService

CRUD operations and filtered search for expenses.

```java
Page<Expense> getExpenses(ExpenseFilter filter, Pageable pageable)
Expense getExpenseById(Long id, String username)
Expense create(CreateExpenseDTO dto, String username)
Expense update(Long id, CreateExpenseDTO dto, String username)
void delete(Long id, String username)
```

**Business rules:**
- Expenses are user-scoped
- Category must belong to the same user → `NoSuchElementException` if another user's category
- Filtered search supports optional category, date range, and pagination

### BudgetService

Budget management and monthly spending summaries.

```java
List<Budget> getBudgets(String username)
Budget createBudget(BudgetRequest request, String username)
Budget updateBudget(Long id, BigDecimal monthlyLimit, String username)
List<MonthlySummaryDTO> getMonthlySummary(String username, int month, int year)
```

**Business rules:**
- One budget per user per category per month → `IllegalArgumentException` if duplicate
- Category must belong to the same user
- Monthly summary compares actual spending (SUM of expenses) against budget limits

## Service DTOs

### ExpenseFilter

Used by `ExpenseService.getExpenses()` to encapsulate filter parameters.

```java
@Builder
public class ExpenseFilter {
    String username;
    Long categoryId;    // nullable — no filter if null
    LocalDate from;     // nullable — no lower bound if null
    LocalDate to;       // nullable — no upper bound if null
}
```

### CreateExpenseDTO

Used by `ExpenseService.create()` and `update()` to pass expense data from the controller.

```java
public class CreateExpenseDTO {
    BigDecimal amount;
    String description;
    LocalDate date;
    Long categoryId;
}
```

### MonthlySummaryDTO

Returned by `BudgetService.getMonthlySummary()`.

```java
public class MonthlySummaryDTO {
    Long categoryId;
    String categoryName;
    BigDecimal spent;        // SUM of expenses for this category this month
    BigDecimal budgetLimit;  // the budget set for this category this month
    BigDecimal remaining;    // budgetLimit - spent
}
```

## How Monthly Summary Works

This is the most complex business logic in the application:

```
Step 1: Query ExpenseRepository
        "SUM all expenses for user X in month Y, grouped by category"
        → {Food: $55.50, Transport: $15.00}

Step 2: Query BudgetRepository
        "Get all budgets for user X in month Y"
        → {Food: $300, Transport: $100}

Step 3: For each budget, compute remaining = limit - spent
        → Food:      $300 - $55.50 = $244.50
        → Transport:  $100 - $15.00 =  $85.00
```

If a category has expenses but no budget, it doesn't appear in the summary. If a category has a budget but no expenses, spent = $0 and remaining = the full budget limit.

## Transaction Management

- Read-only operations use `@Transactional(readOnly = true)` — Hibernate optimization, no dirty checking
- Write operations use `@Transactional` — ensures atomicity (e.g., budget creation checks for duplicates then inserts — both in one transaction)

## Exception Strategy

Services throw standard Java exceptions. The web module's `GlobalExceptionHandler` maps them to HTTP status codes.

| Exception | Meaning | HTTP Status (in web) |
|---|---|---|
| `NoSuchElementException` | Entity not found or not owned by user | 404 |
| `IllegalArgumentException` | Business rule violation | 400 |

Services never throw HTTP-specific exceptions — they stay pure business logic.

## Dependencies

```xml
<dependency>
    <groupId>com.security.app</groupId>
    <artifactId>persistence</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Spring Security is included for `PasswordEncoder` (used in `UserService`) and `AccessDeniedException` (if needed for authorization checks in service methods).
