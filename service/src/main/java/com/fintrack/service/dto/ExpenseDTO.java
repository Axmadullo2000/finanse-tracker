package com.fintrack.service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;


@Getter
@Builder
public class ExpenseDTO {
    private final String username;
    private final Long categoryId;
    private final LocalDate from;
    private final LocalDate to;
}
