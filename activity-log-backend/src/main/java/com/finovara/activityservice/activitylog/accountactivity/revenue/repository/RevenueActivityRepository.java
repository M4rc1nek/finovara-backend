package com.finovara.activityservice.activitylog.accountactivity.revenue.repository;

import com.finovara.activityservice.activitylog.accountactivity.revenue.model.RevenueActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RevenueActivityRepository extends JpaRepository<RevenueActivity, Long> {

    List<RevenueActivity> findByUserId(Long userId, Pageable pageable);
}
