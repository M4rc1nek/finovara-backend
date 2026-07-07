package com.finovara.financeservice.sharedaccount.controller.expense;

import com.finovara.contracts.model.PeriodType;
import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.dto.ExpenseRequestDto;
import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.sharedaccount.dto.expense.SharedExpenseDto;
import com.finovara.financeservice.sharedaccount.dto.expense.SharedExpenseResponse;
import com.finovara.financeservice.sharedaccount.service.expense.SharedExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/shared/transactions/expense")
@RequiredArgsConstructor
public class SharedExpenseController {

    private final SharedExpenseService expenseService;

    @PostMapping
    public ResponseEntity<SharedExpenseResponse> addSharedExpense(@RequestBody @Valid SharedExpenseDto sharedExpenseDto) {
        return ResponseEntity.ok(expenseService.addExpense(sharedExpenseDto, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/edit/{expenseId}")
    public ResponseEntity<Long> editSharedExpense(@RequestBody @Valid SharedExpenseDto sharedExpenseDto, @PathVariable Long expenseId) {
        return ResponseEntity.ok(expenseService.editExpense(sharedExpenseDto, SecurityUtils.getCurrentUserId(), expenseId));
    }

    @GetMapping
    public ResponseEntity<List<SharedExpenseDto>> getSharedExpense() {
        return ResponseEntity.ok(expenseService.getExpense(SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteSharedExpense(@PathVariable Long expenseId) {
        expenseService.deleteExpense(expenseId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

}
