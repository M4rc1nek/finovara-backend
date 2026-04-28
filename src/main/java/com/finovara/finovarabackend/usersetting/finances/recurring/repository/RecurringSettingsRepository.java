package com.finovara.finovarabackend.usersetting.finances.recurring.repository;

import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringSettingsRepository extends JpaRepository<RecurringSettings, Long> {

    @Query("""
                SELECT rs
                FROM RecurringSettings rs
                WHERE rs.enable = true
                  AND rs.nextExecutionDate IS NOT NULL
                  AND rs.nextExecutionDate <= :today
            """)
    List<RecurringSettings> findDueRecurring(@Param("today") LocalDate today);

    @Query("""
                SELECT rs
                FROM RecurringSettings rs
                WHERE rs.userAssigned.id = :userId
                  AND rs.type = :type
            """)
    Optional<RecurringSettings> findByUserAssignedIdAndType(@Param("userId") Long userId, @Param("type") RecurringType type);
}