package com.finovara.financeservice.wallet.reservation.repository;

import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FundReservationRepository extends JpaRepository<FundReservation, Long> {
    @Query("SELECT fr FROM FundReservation fr WHERE fr.walletId = :walletId")
    List<FundReservation> findAllByWalletId(Long walletId);

    @Query("SELECT fr FROM FundReservation fr WHERE fr.id = :reservationId AND fr.walletId = :walletId")
    Optional<FundReservation> findByIdAndWalletId(Long reservationId, Long walletId);
}