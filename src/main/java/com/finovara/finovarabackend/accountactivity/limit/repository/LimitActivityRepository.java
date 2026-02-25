package com.finovara.finovarabackend.accountactivity.limit.repository;

import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivity;
import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LimitActivityRepository extends JpaRepository<LimitActivity, Long> {

    @Query("SELECT e FROM LimitActivity e WHERE e.userAssigned.email = :email")
    List<LimitActivity> findByUserAssignedEmail(@Param("email") String email, Pageable pageable);
}
