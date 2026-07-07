package com.finovara.financeservice.sharedaccount.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.sharedaccount.dto.revenue.SharedRevenueDto;
import com.finovara.financeservice.sharedaccount.service.revenue.SharedRevenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/shared/transactions/revenue")
@RequiredArgsConstructor
public class SharedRevenueController {
    private final SharedRevenueService sharedRevenueService;

    @PostMapping
    public ResponseEntity<Long> addRevenue(@RequestBody @Valid SharedRevenueDto sharedRevenueDto) {
        return ResponseEntity.ok(sharedRevenueService.addSharedRevenue(sharedRevenueDto, SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/edit/{revenueId}")
    public ResponseEntity<Long> editRevenue(@RequestBody @Valid SharedRevenueDto sharedRevenueDto, @PathVariable Long revenueId) {
        return ResponseEntity.ok(sharedRevenueService.editRevenue(sharedRevenueDto, revenueId, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping
    public ResponseEntity<List<SharedRevenueDto>> getRevenue() {
        return ResponseEntity.ok(sharedRevenueService.getRevenue(SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{revenueId}")
    public ResponseEntity<Void> deleteRevenue(@PathVariable Long revenueId) {
        sharedRevenueService.deleteRevenue(revenueId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
