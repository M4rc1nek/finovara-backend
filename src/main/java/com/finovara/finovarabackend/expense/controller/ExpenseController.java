package com.finovara.finovarabackend.expense.controller;

import com.finovara.finovarabackend.expense.dto.ExpenseDTO;
import com.finovara.finovarabackend.expense.service.ExpenseService;
import com.finovara.finovarabackend.revenue.dto.RevenueDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/addExpense")
    public ResponseEntity<ExpenseDTO> addExpense(@RequestBody @Valid ExpenseDTO expenseDTO) {
        String email = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        return ResponseEntity.ok(expenseService.addExpense(expenseDTO, email));
    }

    @PutMapping("/editExpense/{expenseId}")
    public ResponseEntity<ExpenseDTO> editExpense(@RequestBody @Valid ExpenseDTO expenseDTO, @PathVariable Long expenseId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(expenseService.editExpense(expenseDTO, email, expenseId));
    }

    @DeleteMapping("/deleteExpense/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        expenseService.deleteExpense(expenseId, email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getExpense")
    public ResponseEntity<List<ExpenseDTO>> getExpense(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(expenseService.getExpense(email));
    }




}
