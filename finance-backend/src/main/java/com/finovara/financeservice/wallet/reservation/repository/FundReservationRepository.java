package com.finovara.financeservice.wallet.reservation.repository;

import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FundReservationRepository extends JpaRepository<FundReservation, Long> {
    @Query("SELECT fr FROM FundReservation fr WHERE fr.walletId = :walletId")
    List<FundReservation> findByWalletId(Long walletId);

    @Modifying
    @Query("DELETE FROM FundReservation r WHERE r.walletId IN (SELECT w.id FROM Wallet w WHERE w.userId = :userId)")
    void deleteAllByUserId(Long userId);
}