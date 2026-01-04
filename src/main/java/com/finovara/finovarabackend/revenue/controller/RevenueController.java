package com.finovara.finovarabackend.revenue.controller;

import com.finovara.finovarabackend.revenue.dto.RevenueDTO;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RevenueController {
    private final RevenueService revenueService;

    @PostMapping("/addRevenue")
    public ResponseEntity<RevenueDTO> addRevenue(@RequestBody @Valid RevenueDTO revenueDTO) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(revenueService.addRevenue(revenueDTO, email));
    }

    @PutMapping("/editRevenue/{revenueId}")
    public ResponseEntity<RevenueDTO> editRevenue(@RequestBody @Valid RevenueDTO revenueDTO, @PathVariable Long revenueId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(revenueService.editRevenue(revenueDTO, revenueId, email));
    }

    @DeleteMapping("/deleteRevenue/{revenueId}")
    public ResponseEntity<Void> deleteRevenue(@PathVariable Long revenueId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        revenueService.deleteRevenue(revenueId, email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getRevenue")
    public ResponseEntity<List<RevenueDTO>> getRevenue(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(revenueService.getRevenue(email));
    }
}
