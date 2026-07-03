package com.finovara.financeservice.sharedaccount.repository.wallet;

import com.finovara.financeservice.sharedaccount.model.wallet.SharedWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedWalletRepository extends JpaRepository<SharedWallet, Long> {

    Optional<SharedWallet> findByOwnerIdOrMemberId(Long ownerId, Long memberId);

    boolean existsByOwnerIdOrMemberId(Long ownerId, Long memberId);
}
