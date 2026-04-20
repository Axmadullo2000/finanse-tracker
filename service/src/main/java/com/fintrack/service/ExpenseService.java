package com.fintrack.service;

import com.fintrack.entity.Category;
import com.fintrack.entity.Expense;
import com.fintrack.entity.User;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.ExpenseRepository;
import com.fintrack.repository.UserRepository;

import com.fintrack.service.dto.CreateExpenseDTO;
import com.fintrack.service.dto.ExpenseDTO;
import com.fintrack.service.mapper.ExpenseMapperService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;


@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseMapperService expenseMapper;

    public Page<Expense> getExpenses(ExpenseDTO filter, Pageable pageable) {
        return expenseRepository.findFiltered(
                filter.getUsername(),
                filter.getCategoryId(),
                filter.getFrom(),
                filter.getTo(),
                pageable
        );
    }

    public Expense getExpenseById(Long id, String username) {
        return expenseRepository.findByIdAndUser_Username(id, username)
            .orElseThrow(() ->
            new NoSuchElementException("Expense not found with id: %d"
            .formatted(id)));
    }

    public Expense create(CreateExpenseDTO dto, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("User not found with username: %s"
                    .formatted(username)));

        Category category = categoryRepository.findByIdAndUser_Username(dto.getCategoryId(), username)
            .orElseThrow(() -> new NoSuchElementException("Category not found with categoryId: %d"
                    .formatted(dto.getCategoryId())));

        Expense expense = new Expense(dto.getAmount(), dto.getDescription(), dto.getDate(), category, user);
        return expenseRepository.save(expense);
    }

    public Expense update(Long id, CreateExpenseDTO expenseDTO, String username) {

        Expense foundExpense = getExpenseById(id, username);

        Category category = categoryRepository.findByIdAndUser_Username(expenseDTO.getCategoryId(), username)
                .orElseThrow(() -> new NoSuchElementException(
                        "Category not found with id: %d".formatted(expenseDTO.getCategoryId())));

        Expense expense = expenseMapper.map(foundExpense, expenseDTO, category);
        return expenseRepository.save(expense);
    }

    public void delete(Long id, String username) {
        Expense expense = getExpenseById(id, username);
        expenseRepository.delete(expense);
    }

}
