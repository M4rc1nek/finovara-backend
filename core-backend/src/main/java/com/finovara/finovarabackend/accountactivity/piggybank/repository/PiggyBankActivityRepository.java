package com.finovara.finovarabackend.accountactivity.piggybank.repository;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PiggyBankActivityRepository extends JpaRepository<PiggyBankActivity, Long> {

    @Query("SELECT e FROM PiggyBankActivity e WHERE e.userAssigned.email = :email")
    List<PiggyBankActivity> findByUserAssignedEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT e FROM PiggyBankActivity e WHERE e.userAssigned.id = :userId")
    List<PiggyBankActivity> findByUserAssignedId(@Param("userId") Long userId, Pageable pageable);
}
