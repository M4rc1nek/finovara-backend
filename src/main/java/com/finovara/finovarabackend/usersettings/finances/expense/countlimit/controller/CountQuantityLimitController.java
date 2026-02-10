package com.finovara.finovarabackend.usersettings.finances.expense.countlimit.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.service.CountQuantityLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expense-settings/count-quantity-expense")
@RequiredArgsConstructor
public class CountQuantityLimitController {

    private final CountQuantityLimitService countQuantityLimitService;

    @PutMapping
    public ResponseEntity<Void> saveCountQuantityExpenseLimit(@RequestBody CountQuantityLimitDto countQuantityLimitDto) {
        countQuantityLimitService.saveCountQuantityLimit(SecurityUtils.getCurrentUserEmail(), countQuantityLimitDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<CountQuantityLimitDto> getCountQuantityExpenseLimit() {
        return ResponseEntity.ok(countQuantityLimitService.getCountQuantityLimit(SecurityUtils.getCurrentUserEmail()));
    }
}
