package com.finovara.finovarabackend.revenue.controller;

import com.finovara.finovarabackend.revenue.dto.RevenueDTO;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.finovara.finovarabackend.security.SecurityUtils.getCurrentUserEmail;

@RestController
@RequiredArgsConstructor
public class RevenueController {
    private final RevenueService revenueService;

    @PostMapping("/addRevenue")
    public ResponseEntity<Long> addRevenue(@RequestBody @Valid RevenueDTO revenueDTO) {
        return ResponseEntity.ok(revenueService.addRevenue(revenueDTO, getCurrentUserEmail()));
    }

    @PutMapping("/editRevenue/{revenueId}")
    public ResponseEntity<Long> editRevenue(@RequestBody @Valid RevenueDTO revenueDTO, @PathVariable Long revenueId) {
        return ResponseEntity.ok(revenueService.editRevenue(revenueDTO, revenueId, getCurrentUserEmail()));
    }

    @DeleteMapping("/deleteRevenue/{revenueId}")
    public ResponseEntity<Void> deleteRevenue(@PathVariable Long revenueId) {
        revenueService.deleteRevenue(revenueId, getCurrentUserEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getRevenue")
    public ResponseEntity<List<RevenueDTO>> getRevenue() {
        return ResponseEntity.ok(revenueService.getRevenue(getCurrentUserEmail()));
    }
}
