package com.finovara.activitylogservice.activitylog.mainaccount.wallet.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.wallet.dto.WalletStatusActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.wallet.model.WalletStatusActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WalletStatusActivityRepository extends JpaRepository<WalletStatusActivity, Long> {

    List<WalletStatusActivity> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT a FROM WalletStatusActivity a
            WHERE a.userId = :userId
              AND (CAST(:#{#filter.type} AS string) IS NULL OR a.type = :#{#filter.type})
              AND (CAST(:#{#filter.reason} AS string) IS NULL OR a.reason = :#{#filter.reason})
              AND (CAST(:#{#filter.amount} AS string) IS NULL OR a.amount = :#{#filter.amount})
              AND (CAST(:createdAt AS date) IS NULL OR CAST(a.createdAt AS date) = :createdAt)
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<WalletStatusActivity> findFilteredActivities(Long userId, WalletStatusActivityFilterDto filter, LocalDate createdAt, Pageable pageable);

    List<WalletStatusActivity> findAllByUserId(Long userId);
}
