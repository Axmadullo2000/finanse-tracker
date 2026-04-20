package com.fintrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BudgetResponse {

    private Long id;
    private BigDecimal monthlyLimit;
    private Integer month;
    private Integer year;
    private Long categoryId;
    private String categoryName;

}
