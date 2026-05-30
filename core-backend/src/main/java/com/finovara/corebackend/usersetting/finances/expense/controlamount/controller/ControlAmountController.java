package com.finovara.corebackend.usersetting.finances.expense.controlamount.controller;

import com.finovara.corebackend.security.SecurityUtils;
import com.finovara.corebackend.usersetting.finances.expense.controlamount.dto.ControlAmountDto;
import com.finovara.corebackend.usersetting.finances.expense.controlamount.service.ControlAmountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expense-settings/expense-control-amount")
@RequiredArgsConstructor
public class ControlAmountController {

    private final ControlAmountService controlAmountService;

    @PatchMapping
    public ResponseEntity<Void> saveExpenseAmountControl(@RequestBody @Valid ControlAmountDto controlAmountDto) {
        controlAmountService.saveExpenseAmountControl(SecurityUtils.getCurrentUserId(), controlAmountDto);
        return ResponseEntity.noContent().build();
    }
    @GetMapping
    public ResponseEntity<ControlAmountDto> getExpenseAmountControl() {
        return ResponseEntity.ok(controlAmountService.getExpenseAmountControl(SecurityUtils.getCurrentUserId()));
    }

}
