package com.finovara.corebackend.wallet.repository;

import com.finovara.corebackend.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserAssignedEmail(String email);

    Optional<Wallet> findByUserAssignedId(Long userId);
}
