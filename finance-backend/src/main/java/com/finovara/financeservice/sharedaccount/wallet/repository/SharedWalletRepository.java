package com.finovara.financeservice.sharedaccount.wallet.repository;

import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedWalletRepository extends JpaRepository<SharedWallet, Long> {

    @Query("SELECT sw FROM SharedWallet sw WHERE sw.ownerId = :ownerId OR sw.memberId = :memberId")
    Optional<SharedWallet> findByOwnerIdOrMemberId(Long ownerId, Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sw FROM SharedWallet sw WHERE sw.ownerId = :ownerId OR sw.memberId = :memberId")
    Optional<SharedWallet> findByOwnerIdOrMemberIdForUpdate(Long ownerId, Long memberId);

    @Query("SELECT sw FROM SharedWallet sw WHERE sw.ownerId = :userId OR sw.memberId = :userId")
    SharedWallet findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM SharedWallet sw WHERE sw.ownerId = :ownerId AND sw.memberId = :memberId")
    void deleteByOwnerIdAndMemberId(Long ownerId, Long memberId);

    boolean existsByOwnerIdAndMemberId(Long ownerId, Long memberId);

}