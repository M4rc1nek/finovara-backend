package com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.repository;

import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.model.SharedAccountActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SharedAccountActivityRepository extends JpaRepository<SharedAccountActivity, Long> {

    List<SharedAccountActivity> findByUserId(Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}
