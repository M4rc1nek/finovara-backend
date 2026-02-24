package com.finovara.finovarabackend.accountactivity.revenue.repository;

import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivity;
import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RevenueActivityRepository extends JpaRepository<RevenueActivity, Long> {

    @Query("SELECT e FROM RevenueActivity e WHERE e.userAssigned.email = :email")
    List<RevenueActivity> findByUserAssignedEmail(@Param("email") String email, Pageable pageable);

}
