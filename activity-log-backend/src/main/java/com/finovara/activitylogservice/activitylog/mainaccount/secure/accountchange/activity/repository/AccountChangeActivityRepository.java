package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.model.AccountChangeActivity;
import com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccountChangeActivityRepository extends JpaRepository<AccountChangeActivity, Long> {

    List<AccountChangeActivity> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT COUNT(u) FROM AccountChangeActivity u WHERE u.userId = :userId")
    long countAccountChangesByUserId(Long userId);

    @Query("""
            SELECT COUNT(u)
            FROM AccountChangeActivity u
            WHERE u.userId = :userId
              AND u.type = :type
              AND u.createdAt >= :from
              AND u.createdAt <= :to
            """)
    long countByUserIdAndStatusAndCreatedAtBetween(Long userId, AccountChangeActivityType type, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT MAX(u.createdAt)
            FROM AccountChangeActivity u
            WHERE u.userId = :userId
              AND u.type = :type
              AND u.createdAt >= :from
              AND u.createdAt <= :to
            """)
    LocalDateTime findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(Long userId, AccountChangeActivityType type, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT COUNT(a)
            FROM AccountChangeActivity a
            WHERE a.userId = :userId
              AND a.type = :type
              AND a.createdAt >= :from
              AND a.createdAt < :to
            """)
    long countByUserIdAndTypeAndCreatedAtBetween(Long userId, AccountChangeActivityType type, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT a
            FROM AccountChangeActivity a
            WHERE a.userId = :userId
              AND a.type = :type
              AND a.createdAt >= :from
              AND a.createdAt < :to
            ORDER BY a.createdAt DESC
            """)
    List<AccountChangeActivity> findActivities(Long userId, AccountChangeActivityType type, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("""
            SELECT a
            FROM AccountChangeActivity a
            WHERE a.userId = :userId
              AND a.type IN (
                  com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType.ADDITIONAL_AUTHORIZATION_ENABLED,
                  com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType.ADDITIONAL_AUTHORIZATION_DISABLED
              )
            ORDER BY a.createdAt DESC
            """)
    List<AccountChangeActivity> findAuthorizationStatusChanges(Long userId, Pageable pageable);

    @Query("SELECT u FROM AccountChangeActivity u WHERE u.userId = :userId ORDER BY u.id")
    List<AccountChangeActivity> findFewByUserId(Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}
