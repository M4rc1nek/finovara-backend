package com.finovara.activitylogservice.activitylog.mainaccountactivity.sharedaccount.repository;

import com.finovara.activitylogservice.activitylog.mainaccountactivity.sharedaccount.model.SharedAccountChangeHistoryActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SharedAccountChangeHistoryActivityRepository extends JpaRepository<SharedAccountChangeHistoryActivity, Long> {

    List<SharedAccountChangeHistoryActivity> findByUserId(Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}
