package com.finovara.financeservice.sharedaccount.wallet.repository;

import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedWalletRepository extends JpaRepository<SharedWallet, Long> {

    @Query("SELECT sw FROM SharedWallet sw WHERE sw.ownerId = :ownerId OR sw.memberId = :memberId")
    Optional<SharedWallet> findByOwnerIdOrMemberId(Long ownerId, Long memberId);

    @Query("SELECT e FROM SharedWallet e WHERE e.ownerId = :ownerId AND e.memberId = :memberId")
    Optional<SharedWallet> findByOwnerIdAndMemberId(Long ownerId, Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sw FROM SharedWallet sw WHERE sw.ownerId = :ownerId OR sw.memberId = :memberId")
    Optional<SharedWallet> findByOwnerIdOrMemberIdForUpdate(Long ownerId, Long memberId);

    @Query("SELECT sw FROM SharedWallet sw WHERE sw.ownerId = :userId OR sw.memberId = :userId")
    SharedWallet findByUserId(Long userId);

    boolean existsByOwnerIdAndMemberId(Long ownerId, Long memberId);

}