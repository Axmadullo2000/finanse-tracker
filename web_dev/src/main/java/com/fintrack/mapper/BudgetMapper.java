package com.fintrack.mapper;

import com.fintrack.dto.BudgetResponse;
import com.fintrack.entity.Budget;

import org.springframework.stereotype.Component;


@Component
public class BudgetMapper {

    public BudgetResponse toResponse(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getMonthlyLimit(),
                budget.getMonth(),
                budget.getYear(),
                budget.getCategory().getId(),
                budget.getCategory().getName()
        );
    }

}
