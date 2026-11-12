package com.finovara.financeservice.wallet.reservation.repository;

import com.finovara.financeservice.wallet.reservation.model.FundReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FundReservationRepository extends JpaRepository<FundReservation, Long> {

    @Query("SELECT fr FROM FundReservation fr WHERE fr.walletId = :walletId AND fr.deletedAt IS NULL")
    List<FundReservation> findAllByWalletId(Long walletId);

    @Query("SELECT fr FROM FundReservation fr, Wallet w WHERE fr.walletId = w.id AND w.userId = :userId")
    List<FundReservation> findAllByUserId(Long userId);

    @Query("SELECT fr FROM FundReservation fr WHERE fr.id = :reservationId AND fr.walletId = :walletId AND fr.deletedAt IS NULL")
    Optional<FundReservation> findByIdAndWalletId(Long reservationId, Long walletId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fr FROM FundReservation fr WHERE fr.id = :reservationId AND fr.walletId = :walletId AND fr.deletedAt IS NOT NULL")
    Optional<FundReservation> findTrashedByIdAndWalletIdForUpdate(Long reservationId, Long walletId);

    @Query("SELECT reservation FROM FundReservation reservation WHERE reservation.id IN :ids AND reservation.deletedAt IS NOT NULL")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<FundReservation> findAllByIdInAndDeletedAtIsNotNull(List<Long> ids);
}
