package com.fintrack.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class MonthlySummaryResponse {

    private Long categoryId;
    private String categoryName;
    private BigDecimal spent;
    private BigDecimal budgetLimit;
    private BigDecimal remaining;
}
