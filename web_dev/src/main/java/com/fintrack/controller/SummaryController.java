package com.fintrack.controller;

import com.fintrack.dto.MonthlySummaryResponse;
import com.fintrack.security.AuthenticatedUser;
import com.fintrack.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final BudgetService budgetService;
    private final AuthenticatedUser authenticatedUser;

    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlySummaryResponse>> getMonthlySummary(
            @RequestParam int month,
            @RequestParam int year) {

        String username = authenticatedUser.getUsername();
        List<MonthlySummaryResponse> summary = budgetService
                .getMonthlySummary(username, month, year)
                .stream()
                .map(dto -> new MonthlySummaryResponse(
                        dto.getCategoryId(),
                        dto.getCategoryName(),
                        dto.getSpent(),
                        dto.getBudgetLimit(),
                        dto.getRemaining()))
                .toList();

        return ResponseEntity.ok(summary);
    }
}
