package com.finovara.authbackend.revenue.controller;

import com.finovara.authbackend.revenue.dto.RevenueDto;
import com.finovara.authbackend.revenue.service.RevenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.finovara.authbackend.security.SecurityUtils.getCurrentUserId;

@RestController
@RequiredArgsConstructor
public class RevenueController {
    private final RevenueService revenueService;

    @PostMapping("/addRevenue")
    public ResponseEntity<Long> addRevenue(@RequestBody @Valid RevenueDto revenueDto) {
        return ResponseEntity.ok(revenueService.addRevenue(revenueDto, getCurrentUserId()));
    }

    @PutMapping("/editRevenue/{revenueId}")
    public ResponseEntity<Long> editRevenue(@RequestBody @Valid RevenueDto revenueDto, @PathVariable Long revenueId) {
        return ResponseEntity.ok(revenueService.editRevenue(revenueDto, revenueId, getCurrentUserId()));
    }

    @DeleteMapping("/deleteRevenue/{revenueId}")
    public ResponseEntity<Void> deleteRevenue(@PathVariable Long revenueId) {
        revenueService.deleteRevenue(revenueId, getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getRevenue")
    public ResponseEntity<List<RevenueDto>> getRevenue() {
        return ResponseEntity.ok(revenueService.getRevenue(getCurrentUserId()));
    }
}
