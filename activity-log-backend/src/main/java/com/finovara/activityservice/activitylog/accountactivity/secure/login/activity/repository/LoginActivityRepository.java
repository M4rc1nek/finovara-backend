package com.finovara.activityservice.activitylog.accountactivity.secure.login.activity.repository;

import com.finovara.activityservice.activitylog.accountactivity.secure.login.activity.dto.LoginActivityDto;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.activity.model.LoginActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginActivityRepository extends JpaRepository<LoginActivity, Long> {

    @Query("""
            SELECT new com.finovara.activityservice.activity_log.accountactivity.secure.login.activity.dto.LoginActivityDto(
            l.type, l.status, l.createdAt, l.browser, l.ipAddress, l.location)
            FROM LoginActivity l
            WHERE l.userId = :userId
            ORDER BY l.id DESC
            """)
    List<LoginActivityDto> findByUserIdOrderByDesc(@Param("userId") Long userId);

    @Query("SELECT u FROM LoginActivity u WHERE u.userId = :userId ORDER BY u.id ASC")
    List<LoginActivity> findOldestByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(u) FROM LoginActivity u WHERE u.userId = :userId")
    long countActivityLoginByUserId(@Param("userId") Long userId);

}
