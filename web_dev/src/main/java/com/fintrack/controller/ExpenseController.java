package com.fintrack.controller;

import com.fintrack.service.dto.CreateExpenseDTO;
import com.fintrack.dto.ExpenseRequest;
import com.fintrack.dto.ExpenseResponse;
import com.fintrack.entity.Expense;
import com.fintrack.mapper.ExpenseMapper;
import com.fintrack.security.AuthenticatedUser;
import com.fintrack.service.dto.ExpenseDTO;
import com.fintrack.service.ExpenseService;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/expenses")
public class ExpenseController {

    private static final Logger log = LoggerFactory.getLogger(ExpenseController.class);
    private final ExpenseService expenseService;
    private final AuthenticatedUser authenticatedUser;
    private final ExpenseMapper mapper;

    @GetMapping
    public ResponseEntity<Page<ExpenseResponse>> getExpenses(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 10, sort = "date") Pageable pageable
            ) {
        String username = authenticatedUser.getUsername();

        ExpenseDTO filter = ExpenseDTO.builder()
                .username(username)
                .categoryId(categoryId)
                .from(from)
                .to(to)
                .build();

        Page<ExpenseResponse> expenses = expenseService
                .getExpenses(filter, pageable)
                .map(mapper::toResponse);

        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpense(@PathVariable Long id) {
        String username = authenticatedUser.getUsername();
        Expense expense = expenseService.getExpenseById(id, username);
        return ResponseEntity.ok(mapper.toResponse(expense));
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @RequestBody ExpenseRequest request) {
        String username = authenticatedUser.getUsername();
        CreateExpenseDTO expenseDTO = new CreateExpenseDTO(
                request.getAmount(),
                request.getDescription(),
                request.getDate(),
                request.getCategoryId()
        );

        Expense expense = expenseService.create(
                expenseDTO,
                username
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(expense));
    }

    @PutMapping("{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(@PathVariable Long id,
                                                         @RequestBody ExpenseRequest request) {
        String username = authenticatedUser.getUsername();

        CreateExpenseDTO expenseDTO = new CreateExpenseDTO(
                request.getAmount(),
                request.getDescription(),
                request.getDate(),
                request.getCategoryId()
        );

        Expense expense = expenseService.update(
                id,
                expenseDTO,
                username
        );
        return ResponseEntity.status(200)
                .body(mapper.toResponse(expense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ExpenseResponse> deleteExpense(@PathVariable Long id) {
        String username = authenticatedUser.getUsername();
        expenseService.delete(id, username);
        return ResponseEntity.status(204).build();
    }



}
