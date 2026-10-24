package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.model.LoginActivity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginActivityRepository extends JpaRepository<LoginActivity, Long> {

    List<LoginActivity> findByUserId(Long userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM LoginActivity a
            WHERE a.userId = :userId
            ORDER BY a.createdAt, a.id
            """)
    List<LoginActivity> findOldestForArchive(Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}