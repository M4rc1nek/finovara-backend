package com.finovara.activityservice.activity_log.accountactivity.limit.repository;

import com.finovara.activityservice.activity_log.accountactivity.limit.model.LimitActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LimitActivityRepository extends JpaRepository<LimitActivity, Long> {

    List<LimitActivity> findByUserId(Long userId, Pageable pageable);
}
