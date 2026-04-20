# Persistence Module

The data access layer of FinTrack. Contains JPA entities and Spring Data repositories.

## Responsibility

- Define the database schema through JPA entity annotations
- Provide repository interfaces for CRUD operations and custom queries
- No business logic, no HTTP awareness — pure data access

## Entities

### User (`app_user` table)

```java
@Entity
@Table(name = "app_user")
public class User {
    Long id;
    String username;       // unique, not null
    String password;       // BCrypt encoded, not null
    String email;          // unique, not null
    Role role;             // USER or ADMIN
    List<Category> categories;
    List<Expense> expenses;
    List<Budget> budgets;
}
```

Table is named `app_user` because `user` is a reserved keyword in PostgreSQL.

### Role (enum)

```java
public enum Role {
    USER,
    ADMIN
}
```

### Category

```java
@Entity
public class Category {
    Long id;
    String name;           // not null
    User user;             // ManyToOne, LAZY
    List<Expense> expenses;
    List<Budget> budgets;
}
```

### Expense

```java
@Entity
public class Expense {
    Long id;
    BigDecimal amount;     // not null
    String description;
    LocalDate date;        // not null
    Category category;     // ManyToOne, LAZY
    User user;             // ManyToOne, LAZY
}
```

### Budget

```java
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
    columnNames = {"user_id", "category_id", "month", "year"}))
public class Budget {
    Long id;
    BigDecimal monthlyLimit;  // not null
    Integer month;            // 1-12, not null
    Integer year;             // not null
    Category category;        // ManyToOne, LAZY
    User user;                // ManyToOne, LAZY
}
```

The unique constraint ensures one budget per user per category per month.

## Repositories

### UserRepository

```java
Optional<User> findByUsername(String username);
boolean existsByUsername(String username);
boolean existsByEmail(String email);
```

### CategoryRepository

```java
List<Category> findByUserUsername(String username);
Optional<Category> findByIdAndUserUsername(Long id, String username);
boolean existsByNameAndUserUsername(String name, String username);
```

### ExpenseRepository

```java
Optional<Expense> findByIdAndUserUsername(Long id, String username);

// Dynamic filtering with CAST for PostgreSQL null parameter handling
@Query("SELECT e FROM Expense e WHERE e.user.username = :username"
    + " AND (CAST(:categoryId AS long) IS NULL OR e.category.id = :categoryId)"
    + " AND (CAST(:from AS date) IS NULL OR e.date >= :from)"
    + " AND (CAST(:to AS date) IS NULL OR e.date <= :to)")
Page<Expense> findFiltered(...);

// Aggregation query for monthly spending summary
@Query("SELECT e.category.id, e.category.name, SUM(e.amount) "
    + "FROM Expense e WHERE e.user.username = :username "
    + "AND MONTH(e.date) = :month AND YEAR(e.date) = :year "
    + "GROUP BY e.category.id, e.category.name")
List<Object[]> findMonthlySpendingByCategory(...);
```

### BudgetRepository

```java
List<Budget> findByUserUsername(String username);
List<Budget> findByUserUsernameAndMonthAndYear(String username, Integer month, Integer year);
Optional<Budget> findByIdAndUserUsername(Long id, String username);
Optional<Budget> findByCategoryIdAndUserUsernameAndMonthAndYear(...);
```

## Design Decisions

**`FetchType.LAZY` on all `@ManyToOne`** — Prevents unnecessary joins. The default `EAGER` would load the parent entity every time you query a child, causing N+1 problems.

**`orphanRemoval = true` on `@OneToMany`** — When an expense is removed from a user's list, JPA automatically deletes the database row.

**`BigDecimal` for money** — Never use `double` or `float` for financial amounts. Floating-point arithmetic produces rounding errors.

**Spring Data query derivation** — Methods like `findByUserUsername` are automatically implemented by Spring Data from the method name. No `@Query` needed for simple lookups.

**`@Query` for complex queries** — Used for filtered search (with nullable parameters) and aggregation (GROUP BY, SUM). PostgreSQL requires CAST for null parameters in JPQL.

## Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```
