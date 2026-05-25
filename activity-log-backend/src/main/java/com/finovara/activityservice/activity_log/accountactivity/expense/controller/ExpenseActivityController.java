package com.finovara.activityservice.activity_log.accountactivity.expense.controller;

import com.finovara.activityservice.activity_log.accountactivity.expense.dto.ExpenseActivityDto;
import com.finovara.activityservice.activity_log.accountactivity.expense.service.ExpenseActivityService;
import com.finovara.activityservice.security.SecurityUtils;
import com.finovara.activityservice.contracts.model.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/account-activity/expense")
@RequiredArgsConstructor
public class ExpenseActivityController {

    private final ExpenseActivityService expenseActivityService;

    @GetMapping
    public ResponseEntity<List<ExpenseActivityDto>> getExpenseActivity(@RequestParam(defaultValue = "NEWEST") SortType sort) {
        return ResponseEntity.ok(expenseActivityService.getExpenseActivity(SecurityUtils.getCurrentUserId(), sort));
    }
}
