package com.fintrack.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class MonthlySummaryDTO {
    private Long categoryId;
    private String categoryName;
    private BigDecimal spent;
    private BigDecimal budgetLimit;
    private BigDecimal remaining;
}
