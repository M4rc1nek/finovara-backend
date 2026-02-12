package com.finovara.finovarabackend.usersettings.finances.expense.controlamount.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.finances.expense.controlamount.dto.ControlAmountDto;
import com.finovara.finovarabackend.usersettings.finances.expense.controlamount.service.ControlAmountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expense-settings/expense-control-amount")
@RequiredArgsConstructor
public class ControlAmountController {

    private final ControlAmountService controlAmountService;

    @PutMapping
    public ResponseEntity<Void> saveExpenseAmountControl(@RequestBody @Valid ControlAmountDto controlAmountDto) {
        controlAmountService.saveExpenseAmountControl(SecurityUtils.getCurrentUserEmail(), controlAmountDto);
        return ResponseEntity.noContent().build();
    }
    @GetMapping
    public ResponseEntity<ControlAmountDto> getExpenseAmountControl() {
        return ResponseEntity.ok(controlAmountService.getExpenseAmountControl(SecurityUtils.getCurrentUserEmail()));
    }

}
