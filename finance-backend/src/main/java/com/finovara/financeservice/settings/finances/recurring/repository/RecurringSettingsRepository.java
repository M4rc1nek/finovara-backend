package com.finovara.financeservice.settings.finances.recurring.repository;

import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringSettingsRepository extends JpaRepository<RecurringSettings, Long> {


    @Query("SELECT rs FROM RecurringSettings rs WHERE rs.userId = :userId")
    Optional<RecurringSettings> findByUserId(Long userId);


    @Query("""
                SELECT rs FROM RecurringSettings rs
                WHERE rs.enable = true
                  AND rs.nextExecutionDate IS NOT NULL
                  AND rs.nextExecutionDate <= :today
            """)
    List<RecurringSettings> findDueRecurring(LocalDate today);

    Optional<RecurringSettings> findByUserIdAndType(Long userId, RecurringType type);

    Optional<RecurringSettings> findByUserIdAndPiggyBankId(Long userId, Long piggyBankId);

    @Query("SELECT rs FROM RecurringSettings rs WHERE rs.enable = true AND rs.userId = :userId")
    List<RecurringSettings> findAllEnabledByUserId(Long userId);

    @Query("""
            SELECT rs FROM RecurringSettings rs
            WHERE rs.userId = :userId
              AND rs.enable = true
              AND rs.nextExecutionDate IS NOT NULL
              AND rs.nextExecutionDate BETWEEN :from AND :to
        """)
    List<RecurringSettings> findUpcomingByUserId(Long userId, LocalDate from, LocalDate to);

    void deleteAllByUserId(Long userId);
}
