package com.finovara.financeservice.wallet.model;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal reservedAmount = BigDecimal.ZERO;


    public Wallet(Long userId) {
        this.userId = userId;
    }

    public static Wallet create(Long userId) {
        return new Wallet(userId);
    }

    public void deposit(BigDecimal amount) {
        validateAmount(amount);
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        validateAmount(amount);

        BigDecimal available = balance.subtract(reservedAmount);
        if (available.compareTo(amount) < 0) {
            throw new InvalidInputException("Insufficient available funds (reserved funds cannot be spent)");
        }

        balance = balance.subtract(amount);
    }

    public void reserve(BigDecimal amount) {
        validateAmount(amount);
        BigDecimal available = balance.subtract(reservedAmount);
        if (available.compareTo(amount) < 0) {
            throw new InvalidInputException("Insufficient available funds");
        }
        reservedAmount = reservedAmount.add(amount);
    }

    public void unreserve(BigDecimal amount) {
        validateAmount(amount);
        reservedAmount = reservedAmount.subtract(amount);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidInputException("Amount must be positive");
        }
    }

    @Column(nullable = false, unique = true)
    private Long userId;
}