package com.finovara.financeservice.sharedaccount.limit.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitDto;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitStatsDto;
import com.finovara.financeservice.sharedaccount.limit.service.SharedLimitManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shared-accounts/limits")
public class SharedLimitController {
    private final SharedLimitManagementService sharedLimitManagementService;

    @PostMapping
    public ResponseEntity<Long> addSharedLimit(@Valid @RequestBody SharedLimitDto sharedLimitDto) {
        return ResponseEntity.ok(sharedLimitManagementService.createSharedLimit(sharedLimitDto, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{sharedLimitId}/edit")
    public ResponseEntity<Long> editSharedLimit(@Valid @RequestBody SharedLimitDto sharedLimitDto, @PathVariable Long sharedLimitId) {
        return ResponseEntity.ok(sharedLimitManagementService.editSharedLimit(sharedLimitDto, sharedLimitId, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping
    public ResponseEntity<List<SharedLimitStatsDto>> getSharedLimits() {
        return ResponseEntity.ok(sharedLimitManagementService.getSharedLimitStats(SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{sharedLimitId}")
    public ResponseEntity<Void> deleteSharedLimit(@PathVariable Long sharedLimitId) {
        sharedLimitManagementService.deleteSharedLimit(SecurityUtils.getCurrentUserId(), sharedLimitId);
        return ResponseEntity.noContent().build();
    }

}