package com.finovara.activitylogservice.activitylog.mainaccount.piggybank.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.piggybank.dto.PiggyBankActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.piggybank.model.PiggyBankActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PiggyBankActivityRepository extends JpaRepository<PiggyBankActivity, Long> {

    List<PiggyBankActivity> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT p FROM PiggyBankActivity p
            WHERE p.userId = :userId
              AND (CAST(:#{#filter.activityType} AS string) IS NULL OR p.activityType = :#{#filter.activityType})
              AND (CAST(:#{#filter.piggyBankName} AS string) IS NULL OR :#{#filter.piggyBankName} = '' OR LOWER(p.piggyBankName) LIKE LOWER(CONCAT('%', :#{#filter.piggyBankName}, '%')))
              AND (CAST(:#{#filter.goalType} AS string) IS NULL OR p.goalType = :#{#filter.goalType})
              AND (CAST(:#{#filter.goalAmount} AS string) IS NULL OR p.goalAmount = :#{#filter.goalAmount})
              AND (CAST(:#{#filter.amountPaid} AS string) IS NULL OR p.amountPaid = :#{#filter.amountPaid})
              AND (CAST(:#{#filter.amountPaidOut} AS string) IS NULL OR p.amountPaidOut = :#{#filter.amountPaidOut})
              AND (CAST(:createdFrom AS timestamp) IS NULL OR p.createdAt >= :createdFrom)
              AND (CAST(:createdTo AS timestamp) IS NULL OR p.createdAt < :createdTo)
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    List<PiggyBankActivity> findFilteredActivities(Long userId, PiggyBankActivityFilterDto filter, LocalDateTime createdFrom, LocalDateTime createdTo,
                                                   Pageable pageable);

    List<PiggyBankActivity> findAllByUserId(Long userId);
}
