package com.fintrack.service.mapper;

import com.fintrack.entity.Category;
import com.fintrack.entity.Expense;
import com.fintrack.service.dto.CreateExpenseDTO;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapperService {

    public Expense map(Expense expense, CreateExpenseDTO dto, Category category) {
        expense.setDescription(dto.getDescription());
        expense.setDate(dto.getDate());
        expense.setAmount(dto.getAmount());
        expense.setCategory(category);

        return expense;
    }

}
