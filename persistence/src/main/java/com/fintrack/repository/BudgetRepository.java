package com.fintrack.repository;

import com.fintrack.entity.Budget;
import com.fintrack.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUser_Username(String userUsername);

    @Query("""
        SELECT b FROM Budget b
         WHERE (b.category.id = :categoryId OR b.category.id IS NULL )
         AND (b.user.username = :username OR b.user.username IS NULL )
         AND (b.month = :month OR b.month IS NULL )
         AND (b.year = :year OR b.year IS NULL )
    """)
    Optional<Budget> findByFilter(Long categoryId,
                                  String username,
                                  Integer month,
                                  Integer year);

    Optional<Budget> findByIdAndUser_Username(Long id, String userUsername);

    List<Budget> findByUserUsernameAndMonthAndYear(String username, int month, int year);
}