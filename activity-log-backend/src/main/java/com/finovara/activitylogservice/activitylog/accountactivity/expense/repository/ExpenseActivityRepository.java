package com.finovara.activitylogservice.activitylog.accountactivity.expense.repository;

import com.finovara.activitylogservice.activitylog.accountactivity.expense.model.ExpenseActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseActivityRepository extends JpaRepository<ExpenseActivity, Long> {

    List<ExpenseActivity> findByUserId(Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}
