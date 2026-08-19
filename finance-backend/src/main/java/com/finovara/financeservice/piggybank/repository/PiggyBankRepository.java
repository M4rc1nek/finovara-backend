package com.finovara.financeservice.piggybank.repository;

import com.finovara.financeservice.piggybank.model.PiggyBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PiggyBankRepository extends JpaRepository<PiggyBank, Long> {
    @Query("SELECT pb FROM PiggyBank pb WHERE pb.id = :piggyBankId AND pb.userId = :userId")
    Optional<PiggyBank> findByIdAndUserId(Long piggyBankId, Long userId);

    @Query("SELECT pb FROM PiggyBank pb WHERE pb.userId = :userId")
    List<PiggyBank> findAllByUserId(Long userId);

    @Query("SELECT COUNT(pb) FROM PiggyBank pb WHERE pb.userId = :userId")
    long countPiggyBanksByUserId(Long userId);

    @Query("SELECT COUNT(pb) FROM PiggyBank pb WHERE pb.userId = :userId AND pb.createdAt BETWEEN :startDate AND :endDate")
    long countPiggyBanksByUserIdAndCreatedAtBetween(Long userId, @Param("startDate") LocalDate from, @Param("endDate") LocalDate to);

    @Query("""
                SELECT COALESCE(SUM(pb.amount), 0)
                FROM PiggyBank pb
                WHERE pb.userId = :userId
                  AND pb.id = :piggyBankId
                  AND pb.createdAt BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumDepositsByPiggyBankIdAndUserIdAndCreatedAtBetween(Long userId, Long piggyBankId,
                                                                    @Param("startDate") LocalDate from, @Param("endDate") LocalDate to);

    @Query("""
                SELECT COUNT(p) > 0 
                FROM PiggyBank p 
                WHERE LOWER(p.name) = LOWER(:name) 
                  AND p.userId = :userId
            """)
    boolean existsByNameIgnoreCase(Long userId, String name);

    void deleteByUserId(Long userId);
}
