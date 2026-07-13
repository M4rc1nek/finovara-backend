package com.finovara.financeservice.sharedaccount.wallet.repository;

import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedWalletRepository extends JpaRepository<SharedWallet, Long> {

    @Query("SELECT sw FROM SharedWallet sw WHERE sw.ownerId = :ownerId OR sw.memberId = :memberId")
    Optional<SharedWallet> findByOwnerIdOrMemberId(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM SharedWallet sw WHERE sw.ownerId = :ownerId AND sw.memberId = :memberId")
    void deleteByOwnerIdAndMemberId(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId);

    boolean existsByOwnerIdAndMemberId(Long ownerId, Long memberId);

}