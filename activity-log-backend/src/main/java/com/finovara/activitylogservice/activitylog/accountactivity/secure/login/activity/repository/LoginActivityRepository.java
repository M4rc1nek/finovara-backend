package com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.repository;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.dto.LoginActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.model.LoginActivity;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginActivityRepository extends JpaRepository<LoginActivity, Long> {

    @Query("""
            SELECT new com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.dto.LoginActivityDto(
            l.type, l.status, l.createdAt, l.browser, l.ipAddress, l.location)
            FROM LoginActivity l
            WHERE l.userId = :userId
            ORDER BY l.id DESC
            """)
    List<LoginActivityDto> findByUserIdOrderByDesc(Long userId);

    @Query("SELECT u FROM LoginActivity u WHERE u.userId = :userId ORDER BY u.id ASC")
    List<LoginActivity> findOldestByUserId(Long userId, Pageable pageable);

    @Query("SELECT COUNT(u) FROM LoginActivity u WHERE u.userId = :userId")
    long countActivityLoginByUserId(Long userId);

    @Query("""
            SELECT COUNT(l)
            FROM LoginActivity l
            WHERE l.userId = :userId
              AND l.status = :status
              AND l.createdAt >= :from
              AND l.createdAt <= :to
            """)
    long countByUserIdAndStatusAndCreatedAtBetween(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT DISTINCT l.ipAddress
        FROM LoginActivity l
        WHERE l.userId = :userId
          AND l.status = :status
          AND l.createdAt >= :from
          AND l.createdAt < :to
        """)
    List<String> findDistinctIpAddresses(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT DISTINCT l.browser
        FROM LoginActivity l
        WHERE l.userId = :userId
          AND l.status = :status
          AND l.createdAt >= :from
          AND l.createdAt < :to
        """)
    List<String> findDistinctBrowsers(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT DISTINCT l.location
        FROM LoginActivity l
        WHERE l.userId = :userId
          AND l.status = :status
          AND l.createdAt >= :from
          AND l.createdAt < :to
        """)
    List<String> findDistinctLocations(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to);

    void deleteByUserId(Long userId);

}
