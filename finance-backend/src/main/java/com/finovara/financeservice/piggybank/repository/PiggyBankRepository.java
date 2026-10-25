package com.finovara.financeservice.piggybank.repository;

import com.finovara.financeservice.piggybank.model.PiggyBank;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PiggyBankRepository extends JpaRepository<PiggyBank, Long> {

    @Query("SELECT pb FROM PiggyBank pb WHERE pb.id = :piggyBankId AND pb.userId = :userId")
    Optional<PiggyBank> findByIdAndUserId(Long piggyBankId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pb FROM PiggyBank pb WHERE pb.id = :piggyBankId AND pb.userId = :userId")
    Optional<PiggyBank> findByIdAndUserIdForUpdate(Long piggyBankId, Long userId);

    @Query("SELECT pb FROM PiggyBank pb WHERE pb.userId = :userId ORDER BY pb.createdAt DESC, pb.id DESC")
    List<PiggyBank> findAllByUserId(Long userId);

    @Query("SELECT LOWER(pb.name) FROM PiggyBank pb WHERE pb.userId = :userId")
    List<String> findLowerCaseNamesByUserId(Long userId);

    @Query("SELECT COUNT(pb) FROM PiggyBank pb WHERE pb.userId = :userId AND pb.createdAt BETWEEN :startDate AND :endDate")
    long countPiggyBanksByUserIdAndCreatedAtBetween(Long userId, @Param("startDate") LocalDate from, @Param("endDate") LocalDate to);

    @Query("""
                SELECT COUNT(p) > 0
                FROM PiggyBank p
                WHERE LOWER(p.name) = LOWER(:name)
                  AND p.userId = :userId
                  AND p.id <> :excludedId
            """)
    boolean existsByNameIgnoreCaseExcludingId(Long userId, String name, Long excludedId);

}
