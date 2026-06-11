package com.finovara.activitylogservice.activitylog.accountactivity.piggybank.repository;

import com.finovara.activitylogservice.activitylog.accountactivity.piggybank.model.PiggyBankActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PiggyBankActivityRepository extends JpaRepository<PiggyBankActivity, Long> {

    List<PiggyBankActivity> findByUserId(Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}
