package com.fintrack.repository;

import com.fintrack.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
        SELECT e FROM Expense e WHERE e.user.username = :username
            AND (CAST(:categoryId AS long) IS NULL OR e.category.id = :categoryId)
            AND (CAST(:from AS date) IS NULL OR e.date >= :from)
            AND (CAST(:to AS date) IS NULL OR e.date <= :to)
    """)
    Page<Expense> findFiltered(
            String username,
            Long categoryId,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    Optional<Expense> findByIdAndUser_Username(Long id, String userUsername);

    @Query("SELECT e.category.id, e.category.name, SUM(e.amount) " +
            "FROM Expense e " +
            "WHERE e.user.username = :username " +
            "AND MONTH(e.date) = :month " +
            "AND YEAR(e.date) = :year " +
            "GROUP BY e.category.id, e.category.name")
    List<Object[]> findMonthlySpendingByCategory(
            @Param("username") String username,
            @Param("month") int month,
            @Param("year") int year);

}
