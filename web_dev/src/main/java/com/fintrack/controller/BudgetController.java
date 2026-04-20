package com.fintrack.controller;


import com.fintrack.dto.BudgetRequest;
import com.fintrack.dto.BudgetResponse;
import com.fintrack.entity.Budget;
import com.fintrack.mapper.BudgetMapper;
import com.fintrack.security.AuthenticatedUser;
import com.fintrack.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final AuthenticatedUser authenticatedUser;
    private final BudgetMapper mapper;

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets() {
        String username = authenticatedUser.getUsername();

        List<BudgetResponse> budgets = budgetService.getBudgets(username)
                .stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(budgets);
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @Valid @RequestBody BudgetRequest request) {
        String username = authenticatedUser.getUsername();
        Budget budget = budgetService.createBudget(
                request,
                username
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapper.toResponse(budget));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(@PathVariable Long id,
                                                       @Valid @RequestBody BudgetRequest request) {
        String username = authenticatedUser.getUsername();
        Budget budget = budgetService.update(
                id,
                request.getMonthlyLimit(),
                username
        );

        return ResponseEntity.ok(mapper.toResponse(budget));
    }

}
