package com.finovara.financeservice.wallet.reservation.repository;

import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FundReservationRepository extends JpaRepository<FundReservation, Long> {
    @Query("SELECT fr FROM FundReservation fr WHERE fr.walletId = :walletId")
    List<FundReservation> findByWalletId(Long walletId);
}