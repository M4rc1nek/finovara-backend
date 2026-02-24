package com.finovara.finovarabackend.accountactivity.expense.repository;

import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseActivityRepository extends JpaRepository<ExpenseActivity, Long> {

    @Query("SELECT e FROM ExpenseActivity e WHERE e.userAssigned.email = :email")
    List<ExpenseActivity> findByUserAssignedEmail(@Param("email") String email, Pageable pageable);

}
