package com.finovara.activitylogservice.activitylog.mainaccount.expense.controller;

import com.finovara.activitylogservice.activitylog.mainaccount.expense.dto.ExpenseActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.expense.dto.ExpenseActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.expense.service.ExpenseActivityService;
import com.finovara.activitylogservice.security.SecurityUtils;
import com.finovara.contracts.util.SortType;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/account-activity/expense")
@RequiredArgsConstructor
public class ExpenseActivityController {

    private final ExpenseActivityService expenseActivityService;

    @GetMapping
    public ResponseEntity<List<ExpenseActivityDto>> getExpenseActivity(@RequestParam(defaultValue = "NEWEST") SortType sort,  @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(expenseActivityService.getExpenseActivity(SecurityUtils.getCurrentUserId(), sort, page));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<ExpenseActivityDto>> filterExpenseActivity(@ModelAttribute ExpenseActivityFilterDto filter, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(expenseActivityService.filterExpenseActivity(SecurityUtils.getCurrentUserId(), filter, page));
    }
}
