package com.finovara.financeservice.revenue.suggestions.controller;

import com.finovara.financeservice.revenue.suggestions.dto.RevenueSuggestionDto;
import com.finovara.financeservice.revenue.suggestions.service.RevenueSuggestionService;
import com.finovara.financeservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/suggestion/revenue")
@RequiredArgsConstructor
public class RevenueSuggestionController {

    private final RevenueSuggestionService expenseSuggestionService;

    @GetMapping
    public ResponseEntity<RevenueSuggestionDto> getSuggestion() {
        return ResponseEntity.ok(expenseSuggestionService.getRevenueSuggestion(SecurityUtils.getCurrentUserId()));
    }

}
