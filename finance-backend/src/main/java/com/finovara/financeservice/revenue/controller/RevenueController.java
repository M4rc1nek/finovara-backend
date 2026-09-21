package com.finovara.financeservice.revenue.controller;

import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.service.RevenueService;
import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions/revenue")
@RequiredArgsConstructor
public class RevenueController {
    private final RevenueService revenueService;

    @PostMapping
    public ResponseEntity<Long> addRevenue(@RequestBody @Valid RevenueDto revenueDto, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(revenueService.addRevenue(revenueDto, SecurityUtils.getCurrentUserId(), servletRequest, TransactionOrigin.USER_MANUAL));
    }

    @PutMapping("/edit/{revenueId}")
    public ResponseEntity<Long> editRevenue(@RequestBody @Valid RevenueDto revenueDto, @PathVariable Long revenueId, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(revenueService.editRevenue(revenueDto, revenueId, SecurityUtils.getCurrentUserId(), servletRequest));
    }

    @GetMapping
    public ResponseEntity<List<RevenueDto>> getRevenue() {
        return ResponseEntity.ok(revenueService.getRevenue(SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{revenueId}")
    public ResponseEntity<Void> deleteRevenue(@PathVariable Long revenueId, @RequestParam(required = false) String authorizationCode) {
        revenueService.deleteRevenue(revenueId, SecurityUtils.getCurrentUserId(), authorizationCode);
        return ResponseEntity.noContent().build();
    }
}
