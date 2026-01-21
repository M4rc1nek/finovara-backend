package com.finovara.finovarabackend.limit.controller;

import com.finovara.finovarabackend.limit.dto.LimitDTO;
import com.finovara.finovarabackend.limit.dto.LimitStatsDTO;
import com.finovara.finovarabackend.limit.service.LimitManagementService;
import com.finovara.finovarabackend.security.SecurityUtils;
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
    public ResponseEntity<Long> addLimit(@Valid @RequestBody LimitDTO limitDTO) {
        return ResponseEntity.ok(limitManagementService.createLimit(limitDTO, SecurityUtils.getCurrentUserEmail()));
    }

    @PutMapping("/{limitId}/edit")
    public ResponseEntity<Long> editLimit(@Valid @RequestBody LimitDTO limitDTO, @PathVariable Long limitId) {
        return ResponseEntity.ok(limitManagementService.editLimit(limitDTO, limitId, SecurityUtils.getCurrentUserEmail()));
    }

    @GetMapping
    public ResponseEntity<List<LimitStatsDTO>> getLimits() {
        return ResponseEntity.ok(limitManagementService.getLimitStats(SecurityUtils.getCurrentUserEmail()));
    }

    @DeleteMapping("/{limitId}")
    public ResponseEntity<Void> deleteLimit(@PathVariable Long limitId) {
        limitManagementService.deleteLimit(SecurityUtils.getCurrentUserEmail(), limitId);
        return ResponseEntity.noContent().build();
    }

}
