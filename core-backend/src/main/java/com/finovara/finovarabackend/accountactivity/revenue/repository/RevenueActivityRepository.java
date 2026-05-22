package com.finovara.finovarabackend.accountactivity.revenue.repository;

import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RevenueActivityRepository extends JpaRepository<RevenueActivity, Long> {

    @Query("SELECT e FROM RevenueActivity e WHERE e.userAssigned.id = :userId")
    List<RevenueActivity> findByUserAssignedId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT e FROM RevenueActivity e WHERE e.userAssigned.email = :email")
    List<RevenueActivity> findByUserAssignedEmail(@Param("email") String email, Pageable pageable);

}
