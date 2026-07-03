package com.finovara.financeservice.sharedaccount.model;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "shared_wallets")
public class SharedWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long memberId;

    public SharedWallet(Long ownerId, Long memberId) {
        this.ownerId = ownerId;
        this.memberId = memberId;
    }

    public static SharedWallet create(Long ownerId, Long memberId) {
        return new SharedWallet(ownerId, memberId);
    }

    public void deposit(BigDecimal amount) {
        validateAmount(amount);
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        validateAmount(amount);

        if (balance.compareTo(amount) < 0) {
            throw new InvalidInputException("Insufficient funds");
        }

        balance = balance.subtract(amount);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidInputException("Amount must be positive");
        }
    }
}