package com.finovara.financeservice.wallet.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.wallet.dto.WalletResponse;
import com.finovara.financeservice.wallet.reservation.dto.FundReservationDto;
import com.finovara.financeservice.wallet.reservation.dto.UnreserveReservationDto;
import com.finovara.financeservice.wallet.reservation.service.FundReservationService;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;
    private final FundReservationService fundReservationService;

    @GetMapping("/wallet")
    public ResponseEntity<WalletResponse> getWalletWithReservations() {
        return ResponseEntity.ok(walletService.getWalletWithReservations(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/fund-reservation/create")
    public ResponseEntity<Long> createReservation(@RequestBody FundReservationDto fundReservationDto) {
        return ResponseEntity.ok(fundReservationService.createReservation(SecurityUtils.getCurrentUserId(), fundReservationDto));
    }
    @PatchMapping("/fund-reservation/unreserve/{reservationId}")
    public ResponseEntity<Void> unreserveReservation(@PathVariable Long reservationId, @RequestBody UnreserveReservationDto unreserveReservationDto) {
        fundReservationService.unreserveReservation(SecurityUtils.getCurrentUserId(), reservationId, unreserveReservationDto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/fund-reservation/cancel/{reservationId}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long reservationId) {
        fundReservationService.cancelReservation(SecurityUtils.getCurrentUserId(), reservationId);
        return ResponseEntity.noContent().build();
    }
}
