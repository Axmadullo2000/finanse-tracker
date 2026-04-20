package com.fintrack.mapper;

import com.fintrack.dto.ExpenseResponse;
import com.fintrack.entity.Expense;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {

    public ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getDescription(),
                expense.getDate(),
                expense.getCategory().getId(),
                expense.getCategory().getName()
        );
    }
}
