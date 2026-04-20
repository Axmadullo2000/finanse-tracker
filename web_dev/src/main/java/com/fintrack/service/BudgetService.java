package com.fintrack.service;

import com.fintrack.dto.BudgetRequest;
import com.fintrack.entity.Budget;
import com.fintrack.entity.Category;
import com.fintrack.entity.User;
import com.fintrack.repository.BudgetRepository;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.ExpenseRepository;
import com.fintrack.repository.UserRepository;
import com.fintrack.service.dto.MonthlySummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    public List<Budget> getBudgets(String username) {
        return budgetRepository.findByUser_Username(username);
    }

    @Transactional
    public Budget createBudget(BudgetRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found with username: " + username));

        Category category = categoryRepository.findByIdAndUser_Username(request.getCategoryId(), username)
                .orElseThrow(() -> new NoSuchElementException("Category not found with id: " + request.getCategoryId()));

        budgetRepository.findByFilter(
                request.getCategoryId(), username, request.getMonth(), request.getYear()
        ).ifPresent(budget -> {
            throw new IllegalArgumentException(
                "Budget already exists for %s in %d / %d"
                    .formatted(
                            request.getCategoryId(),
                            request.getMonth(),
                            request.getYear())
            );
        });

        Budget budget = new Budget(
                request.getMonthlyLimit(),
                request.getMonth(),
                request.getYear(),
                category,
                user
        );

        return budgetRepository.save(budget);
    }

    @Transactional
    public Budget update(Long id, BigDecimal monthlyLimit, String username) {
        Budget budget = budgetRepository.findByIdAndUser_Username(id, username)
                .orElseThrow(() -> new NoSuchElementException("Budget not found with id: " + id));

        budget.setMonthlyLimit(monthlyLimit);
        return budgetRepository.save(budget);
    }

    @Transactional(readOnly = true)
    public List<MonthlySummaryDTO> getMonthlySummary(String username, int month, int year) {
        // Step 1: How much spent per category?
        List<Object[]> spending = expenseRepository
                .findMonthlySpendingByCategory(username, month, year);

        Map<Long, BigDecimal> spendingMap = spending.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[2]
                ));

        // Step 2: What budgets exist for this month?
        List<Budget> budgets = budgetRepository
                .findByUserUsernameAndMonthAndYear(username, month, year);

        // Step 3: Compare spending vs budget
        return budgets.stream()
                .map(budget -> {
                    Long catId = budget.getCategory().getId();
                    String catName = budget.getCategory().getName();
                    BigDecimal limit = budget.getMonthlyLimit();
                    BigDecimal spent = spendingMap.getOrDefault(catId, BigDecimal.ZERO);
                    BigDecimal remaining = limit.subtract(spent);

                    return new MonthlySummaryDTO(catId, catName, spent, limit, remaining);
                })
                .toList();
    }
}
