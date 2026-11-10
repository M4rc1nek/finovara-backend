package com.finovara.financeservice.piggybank.repository;

import com.finovara.financeservice.piggybank.model.PiggyBank;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PiggyBankRepository extends JpaRepository<PiggyBank, Long> {

    @Query("SELECT pb FROM PiggyBank pb WHERE pb.id = :piggyBankId AND pb.userId = :userId AND pb.deletedAt IS NULL")
    Optional<PiggyBank> findByIdAndUserId(Long piggyBankId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pb FROM PiggyBank pb WHERE pb.id = :piggyBankId AND pb.userId = :userId AND pb.deletedAt IS NULL")
    Optional<PiggyBank> findByIdAndUserIdForUpdate(Long piggyBankId, Long userId);

    @Query("SELECT pb FROM PiggyBank pb WHERE pb.userId = :userId AND pb.deletedAt IS NULL ORDER BY pb.createdAt DESC, pb.id DESC")
    List<PiggyBank> findAllByUserId(Long userId);

    @Query("SELECT pb FROM PiggyBank pb WHERE pb.userId = :userId")
    List<PiggyBank> findAllByUserIdIncludingDeleted(Long userId);

    @Query("SELECT LOWER(pb.name) FROM PiggyBank pb WHERE pb.userId = :userId AND pb.deletedAt IS NULL")
    List<String> findLowerCaseNamesByUserId(Long userId);

    @Query("SELECT COUNT(pb) FROM PiggyBank pb WHERE pb.userId = :userId AND pb.deletedAt IS NULL AND pb.createdAt BETWEEN :startDate AND :endDate")
    long countPiggyBanksByUserIdAndCreatedAtBetween(Long userId, LocalDate from, LocalDate to);

    @Query("""
                SELECT COUNT(p) > 0
                FROM PiggyBank p
                WHERE LOWER(p.name) = LOWER(:name)
                  AND p.userId = :userId
                  AND p.id <> :excludedId
                  AND p.deletedAt IS NULL
            """)
    boolean existsByNameIgnoreCaseExcludingId(Long userId, String name, Long excludedId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pb FROM PiggyBank pb WHERE pb.id = :id AND pb.userId = :userId AND pb.deletedAt IS NOT NULL")
    Optional<PiggyBank> findTrashedByIdAndUserIdForUpdate(Long id, Long userId);

    @Query("""
            SELECT piggyBank
            FROM PiggyBank piggyBank
            LEFT JOIN FETCH piggyBank.settings
            LEFT JOIN FETCH piggyBank.goalPlanner
            WHERE piggyBank.id IN :ids AND piggyBank.deletedAt IS NOT NULL
            """)
    List<PiggyBank> findAllByIdInAndDeletedAtIsNotNull(List<Long> ids);

}
