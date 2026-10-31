package com.finovara.activitylogservice.activitylog.mainaccount.expense.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.expense.dto.ExpenseActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.expense.model.ExpenseActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExpenseActivityRepository extends JpaRepository<ExpenseActivity, Long> {

    List<ExpenseActivity> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT e FROM ExpenseActivity e
            WHERE e.userId = :userId
              AND (CAST(:#{#filter.type} AS string) IS NULL OR e.type = :#{#filter.type})
              AND (CAST(:#{#filter.amount} AS string) IS NULL OR e.amount = :#{#filter.amount})
              AND (CAST(:#{#filter.previousAmount} AS string) IS NULL OR e.previousAmount = :#{#filter.previousAmount})
              AND (CAST(:#{#filter.category} AS string) IS NULL OR e.category = :#{#filter.category})
              AND (CAST(:#{#filter.previousCategory} AS string) IS NULL OR e.previousCategory = :#{#filter.previousCategory})
              AND (CAST(:createdFrom AS timestamp) IS NULL OR e.createdAt >= :createdFrom)
              AND (CAST(:createdTo AS timestamp) IS NULL OR e.createdAt < :createdTo)
            ORDER BY e.createdAt DESC, e.id DESC
            """)
    List<ExpenseActivity> findFilteredActivities(Long userId, ExpenseActivityFilterDto filter, LocalDateTime createdFrom, LocalDateTime createdTo, Pageable pageable);

    List<ExpenseActivity> findAllByUserId(Long userId);
}
