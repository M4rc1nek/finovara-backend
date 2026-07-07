package com.finovara.financeservice.sharedaccount.repository.wallet;

import com.finovara.financeservice.sharedaccount.model.wallet.SharedWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedWalletRepository extends JpaRepository<SharedWallet, Long> {

    @Query("SELECT sw FROM SharedWallet sw WHERE sw.ownerId = :ownerId OR sw.memberId = :memberId")
    Optional<SharedWallet> findByOwnerIdOrMemberId(Long ownerId, Long memberId);

    @Modifying
    @Query("DELETE FROM SharedWallet sw WHERE sw.ownerId = :ownerId AND sw.memberId = :memberId")
    void deleteByOwnerIdAndMemberId(Long ownerId, Long memberId);

}
