package com.finovara.finovarabackend.expense.controller;

import com.finovara.finovarabackend.expense.dto.ExpenseDTO;
import com.finovara.finovarabackend.expense.service.ExpenseService;
import com.finovara.finovarabackend.limit.model.LimitType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.finovara.finovarabackend.security.SecurityUtils.getCurrentUserEmail;

@RestController
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/addExpense")
    public ResponseEntity<Long> addExpense(@RequestBody @Valid ExpenseDTO expenseDTO, @RequestParam(required = false) LimitType limitType) {
        return ResponseEntity.ok(expenseService.addExpense(expenseDTO, getCurrentUserEmail(), limitType));
    }

    @PutMapping("/editExpense/{expenseId}")
    public ResponseEntity<Long> editExpense(@RequestBody @Valid ExpenseDTO expenseDTO, @PathVariable Long expenseId, @RequestParam(required = false) LimitType limitType) {
        return ResponseEntity.ok(expenseService.editExpense(expenseDTO, getCurrentUserEmail(), expenseId, limitType));
    }

    @DeleteMapping("/deleteExpense/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId) {
        expenseService.deleteExpense(expenseId, getCurrentUserEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getExpense")
    public ResponseEntity<List<ExpenseDTO>> getExpense() {
        return ResponseEntity.ok(expenseService.getExpense(getCurrentUserEmail()));
    }

}
