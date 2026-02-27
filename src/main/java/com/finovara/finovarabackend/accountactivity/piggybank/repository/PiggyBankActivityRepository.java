package com.finovara.finovarabackend.accountactivity.piggybank.repository;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PiggyBankActivityRepository extends JpaRepository<PiggyBankActivity, Long> {

    @Query("SELECT e FROM PiggyBankActivity e WHERE e.userAssigned.email = :email")
    List<PiggyBankActivity> findByUserAssignedEmail(@Param("email") String email, Pageable pageable);
}
