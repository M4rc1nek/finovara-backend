package com.finovara.authbackend.limit.controller;

import com.finovara.authbackend.limit.dto.LimitDto;
import com.finovara.authbackend.limit.dto.LimitStatsDto;
import com.finovara.authbackend.limit.service.LimitManagementService;
import com.finovara.authbackend.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/limits")
public class LimitController {
    private final LimitManagementService limitManagementService;

    @PostMapping
    public ResponseEntity<Long> addLimit(@Valid @RequestBody LimitDto limitDto) {
        return ResponseEntity.ok(limitManagementService.createLimit(limitDto, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{limitId}/edit")
    public ResponseEntity<Long> editLimit(@Valid @RequestBody LimitDto limitDto, @PathVariable Long limitId) {
        return ResponseEntity.ok(limitManagementService.editLimit(limitDto, limitId, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping
    public ResponseEntity<List<LimitStatsDto>> getLimits() {
        return ResponseEntity.ok(limitManagementService.getLimitStats(SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{limitId}")
    public ResponseEntity<Void> deleteLimit(@PathVariable Long limitId) {
        limitManagementService.deleteLimit(SecurityUtils.getCurrentUserId(), limitId);
        return ResponseEntity.noContent().build();
    }

}
