package com.finovara.finovarabackend.wallet.repository;

import com.finovara.finovarabackend.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserAssignedEmail(String email);
}
