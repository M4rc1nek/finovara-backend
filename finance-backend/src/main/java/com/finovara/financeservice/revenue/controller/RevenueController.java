package com.finovara.financeservice.revenue.controller;

import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.service.RevenueFilteringService;
import com.finovara.financeservice.revenue.service.RevenueService;
import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/transactions/revenue")
@RequiredArgsConstructor
public class RevenueController {
    private final RevenueService revenueService;
    private final RevenueFilteringService revenueFilteringService;

    @PostMapping
    public ResponseEntity<Long> addRevenue(@RequestBody @Valid RevenueDto revenueDto, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(revenueService.addRevenue(revenueDto, SecurityUtils.getCurrentUserId(), servletRequest, TransactionOrigin.USER_MANUAL));
    }

    @PutMapping("/edit/{revenueId}")
    public ResponseEntity<Long> editRevenue(@RequestBody @Valid RevenueDto revenueDto, @PathVariable Long revenueId, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(revenueService.editRevenue(revenueDto, revenueId, SecurityUtils.getCurrentUserId(), servletRequest));
    }

    @GetMapping
    public ResponseEntity<List<RevenueDto>> getRevenue(@RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(revenueService.getRevenue(SecurityUtils.getCurrentUserId(), page));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Revenue>> filterRevenues(@RequestParam(required = false) RevenueCategory category, @RequestParam(required = false) BigDecimal amount,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(required = false) String description, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(revenueFilteringService.revenueFiltering(SecurityUtils.getCurrentUserId(), category, amount, date, description, page));
    }

    @DeleteMapping("/{revenueId}")
    public ResponseEntity<Void> deleteRevenue(@PathVariable Long revenueId, @RequestParam(required = false) String authorizationCode) {
        revenueService.deleteRevenue(revenueId, SecurityUtils.getCurrentUserId(), authorizationCode);
        return ResponseEntity.noContent().build();
    }
}

