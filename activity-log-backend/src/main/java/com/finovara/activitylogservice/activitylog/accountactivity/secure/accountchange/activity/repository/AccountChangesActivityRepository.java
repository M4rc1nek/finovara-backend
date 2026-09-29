package com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.repository;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.dto.AccountChangesActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.model.AccountChangesActivity;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountChangesActivityRepository extends JpaRepository<AccountChangesActivity, Long> {

    @Query("""
            SELECT new com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.dto.AccountChangesActivityDto(
            a.type, a.createdAt, a.browser, a.ipAddress, a.location) 
            FROM AccountChangesActivity a 
            WHERE a.userId = :userId
            ORDER BY a.id DESC
            """)
    List<AccountChangesActivityDto> findByUserIdOrderByIdDesc(Long userId);

    @Query("SELECT COUNT(u) FROM AccountChangesActivity u WHERE u.userId = :userId")
    long countAccountChangesByUserId(Long userId);

    @Query("""
            SELECT COUNT(u)
            FROM AccountChangesActivity u
            WHERE u.userId = :userId
              AND u.type = :type
              AND u.createdAt >= :from
              AND u.createdAt <= :to
            """)
    long countByUserIdAndStatusAndCreatedAtBetween(Long userId, AccountChangesActivityType type, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT MAX(u.createdAt)
            FROM AccountChangesActivity u
            WHERE u.userId = :userId
              AND u.type = :type
              AND u.createdAt >= :from
              AND u.createdAt <= :to
            """)
    LocalDateTime findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(Long userId, AccountChangesActivityType type, LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT COUNT(a)
        FROM AccountChangesActivity a
        WHERE a.userId = :userId
          AND a.type = :type
          AND a.createdAt >= :from
          AND a.createdAt < :to
        """)
    long countByUserIdAndTypeAndCreatedAtBetween(Long userId, AccountChangesActivityType type, LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT a
        FROM AccountChangesActivity a
        WHERE a.userId = :userId
          AND a.type = :type
          AND a.createdAt >= :from
          AND a.createdAt < :to
        ORDER BY a.createdAt DESC
        """)
    List<AccountChangesActivity> findActivities(Long userId, AccountChangesActivityType type, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("""
        SELECT a
        FROM AccountChangesActivity a
        WHERE a.userId = :userId
          AND a.type IN (
              com.finovara.contracts.model.activity.AccountChangesActivityType.ADDITIONAL_AUTHORIZATION_ENABLED,
              com.finovara.contracts.model.activity.AccountChangesActivityType.ADDITIONAL_AUTHORIZATION_DISABLED
          )
        ORDER BY a.createdAt DESC
        """)
    List<AccountChangesActivity> findAuthorizationStatusChanges(Long userId, Pageable pageable);

    @Query("SELECT u FROM AccountChangesActivity u WHERE u.userId = :userId ORDER BY u.id")
    List<AccountChangesActivity> findFewByUserId(Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}
