package com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.dto.SharedAccountChangeHistoryActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.model.SharedAccountChangeHistoryActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SharedAccountChangeHistoryActivityRepository extends JpaRepository<SharedAccountChangeHistoryActivity, Long> {

    List<SharedAccountChangeHistoryActivity> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT a FROM SharedAccountChangeHistoryActivity a
            WHERE a.userId = :userId
              AND (CAST(:#{#filter.type} AS string) IS NULL OR a.type = :#{#filter.type})
              AND (CAST(:#{#filter.refundedBalance} AS string) IS NULL OR a.refundedBalance = :#{#filter.refundedBalance})
              AND (CAST(:#{#filter.coFounderUsername} AS string) IS NULL OR :#{#filter.coFounderUsername} = '' OR LOWER(a.coFounderUsername) LIKE LOWER(CONCAT('%', :#{#filter.coFounderUsername}, '%')))
              AND (CAST(:#{#filter.coFounderEmail} AS string) IS NULL OR :#{#filter.coFounderEmail} = '' OR LOWER(a.coFounderEmail) LIKE LOWER(CONCAT('%', :#{#filter.coFounderEmail}, '%')))
              AND (CAST(:createdFrom AS timestamp) IS NULL OR a.createdAt >= :createdFrom)
              AND (CAST(:createdTo AS timestamp) IS NULL OR a.createdAt < :createdTo)
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<SharedAccountChangeHistoryActivity> findFilteredActivities( Long userId, SharedAccountChangeHistoryActivityFilterDto filter, LocalDateTime createdFrom, LocalDateTime createdTo,
                                                                    Pageable pageable);

    List<SharedAccountChangeHistoryActivity> findAllByUserId(Long userId);
}
