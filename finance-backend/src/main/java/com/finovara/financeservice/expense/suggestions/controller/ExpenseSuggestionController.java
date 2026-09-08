package com.finovara.financeservice.expense.suggestions.controller;

import com.finovara.financeservice.expense.suggestions.dto.ExpenseSuggestionDto;
import com.finovara.financeservice.expense.suggestions.service.ExpenseSuggestionService;
import com.finovara.financeservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/suggestion/expense")
@RequiredArgsConstructor
public class ExpenseSuggestionController {

    private final ExpenseSuggestionService expenseSuggestionService;

    @GetMapping
    public ResponseEntity<ExpenseSuggestionDto> getSuggestion() {
        return ResponseEntity.ok(expenseSuggestionService.getExpenseSuggestion(SecurityUtils.getCurrentUserId()));
    }

}
