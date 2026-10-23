package com.finovara.financeservice.settings.finances.recurring.repository;

import com.finovara.contracts.util.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringSettingsRepository extends JpaRepository<RecurringSettings, Long> {

    @Query("SELECT recurringSettings FROM RecurringSettings recurringSettings WHERE recurringSettings.userId = :userId")
    List<RecurringSettings> findAllByUserId(Long userId);

    @Query("""
                SELECT recurringSettings.id FROM RecurringSettings recurringSettings
                WHERE recurringSettings.enable = true
                  AND recurringSettings.nextExecutionDate IS NOT NULL
                  AND recurringSettings.nextExecutionDate <= :today
                  AND recurringSettings.id > :lastProcessedSettingsId
                ORDER BY recurringSettings.id
            """)
    List<Long> findDueSettingsIds(LocalDate today, Long lastProcessedSettingsId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT recurringSettings FROM RecurringSettings recurringSettings
                WHERE recurringSettings.id = :settingsId
                  AND recurringSettings.enable = true
                  AND recurringSettings.nextExecutionDate IS NOT NULL
                  AND recurringSettings.nextExecutionDate <= :today
            """)
    Optional<RecurringSettings> findDueByIdForUpdateSkipLocked(Long settingsId, LocalDate today);

    Optional<RecurringSettings> findByUserIdAndType(Long userId, RecurringType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT recurringSettings FROM RecurringSettings recurringSettings
                WHERE recurringSettings.userId = :userId AND recurringSettings.type = :type
            """)
    Optional<RecurringSettings> findByUserIdAndTypeForUpdate(Long userId, RecurringType type);

    Optional<RecurringSettings> findByUserIdAndPiggyBankId(Long userId, Long piggyBankId);

    @Query("SELECT recurringSettings FROM RecurringSettings recurringSettings WHERE recurringSettings.enable = true AND recurringSettings.userId = :userId")
    List<RecurringSettings> findAllEnabledByUserId(Long userId);

    boolean existsByUserIdAndType(Long userId, RecurringType type);
}