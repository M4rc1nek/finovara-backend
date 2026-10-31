package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.dto.LoginActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.model.LoginActivity;
import com.finovara.activitylogservice.internal.security.util.clientinfo.dto.BrowserCountDto;
import com.finovara.activitylogservice.internal.security.util.clientinfo.dto.LocationCountDto;
import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginActivityRepository extends JpaRepository<LoginActivity, Long> {

    List<LoginActivity> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT l FROM LoginActivity l
            WHERE l.userId = :userId
              AND (CAST(:#{#filter.type} AS string) IS NULL OR :#{#filter.type} = '' OR LOWER(CAST(l.type AS string)) LIKE LOWER(CONCAT('%', CAST(:#{#filter.type} AS string), '%')))
              AND (CAST(:#{#filter.status} AS string) IS NULL OR CAST(l.status AS string) = CAST(:#{#filter.status} AS string))
              AND (CAST(:#{#filter.browser} AS string) IS NULL OR :#{#filter.browser} = '' OR LOWER(l.browser) LIKE LOWER(CONCAT('%', :#{#filter.browser}, '%')))
              AND (CAST(:#{#filter.ipAddress} AS string) IS NULL OR :#{#filter.ipAddress} = '' OR LOWER(l.ipAddress) LIKE LOWER(CONCAT('%', :#{#filter.ipAddress}, '%')))
              AND (CAST(:#{#filter.location} AS string) IS NULL OR :#{#filter.location} = '' OR LOWER(l.location) LIKE LOWER(CONCAT('%', :#{#filter.location}, '%')))
              AND (CAST(:createdFrom AS timestamp) IS NULL OR l.createdAt >= :createdFrom)
              AND (CAST(:createdTo AS timestamp) IS NULL OR l.createdAt < :createdTo)
            ORDER BY l.createdAt DESC, l.id DESC
            """)
    List<LoginActivity> findFilteredActivities( Long userId, LoginActivityFilterDto filter, LocalDateTime createdFrom, LocalDateTime createdTo,
                                              Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM LoginActivity a
            WHERE a.userId = :userId
            ORDER BY a.createdAt, a.id
            """)
    List<LoginActivity> findOldestForArchive(Long userId, Pageable pageable);

    @Query("""
            SELECT COUNT(a)
            FROM LoginActivity a
            WHERE a.userId = :userId
              AND a.status = :status
              AND a.createdAt >= :from
              AND a.createdAt <= :to
            """)
    long countByUserIdAndStatusAndCreatedAtBetween(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT COUNT(DISTINCT a.ipAddress)
            FROM LoginActivity a
            WHERE a.userId = :userId
              AND a.status = :status
              AND a.createdAt >= :from
              AND a.createdAt <= :to
            """)
    long countDistinctDevices(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT DISTINCT a.ipAddress
            FROM LoginActivity a
            WHERE a.userId = :userId
              AND a.status = :status
              AND a.createdAt >= :from
              AND a.createdAt <= :to
            """)
    List<String> findDistinctIpAddresses(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT DISTINCT a.location
            FROM LoginActivity a
            WHERE a.userId = :userId
              AND a.status = :status
              AND a.createdAt >= :from
              AND a.createdAt <= :to
            """)
    List<String> findDistinctLocations(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT DISTINCT a.browser
            FROM LoginActivity a
            WHERE a.userId = :userId
              AND a.status = :status
              AND a.createdAt >= :from
              AND a.createdAt <= :to
            """)
    List<String> findDistinctBrowsers(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT a
            FROM LoginActivity a
            WHERE a.userId = :userId
              AND a.status = :status
              AND a.createdAt >= :from
              AND a.createdAt <= :to
            ORDER BY a.createdAt ASC, a.id ASC
            """)
    List<LoginActivity> findFirstLogins(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("""
            SELECT a
            FROM LoginActivity a
            WHERE a.userId = :userId
              AND a.status = :status
              AND a.createdAt >= :from
              AND a.createdAt <= :to
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<LoginActivity> findLastLogins(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("""
            SELECT NEW com.finovara.activitylogservice.internal.security.util.clientinfo.dto.LocationCountDto(a.location, COUNT(a))
            FROM LoginActivity a
            WHERE a.userId = :userId
              AND a.status = :status
              AND a.createdAt >= :from
              AND a.createdAt <= :to
            GROUP BY a.location
            ORDER BY COUNT(a) DESC
            """)
    List<LocationCountDto> findLocationCounts(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT NEW com.finovara.activitylogservice.internal.security.util.clientinfo.dto.BrowserCountDto(a.browser, COUNT(a))
            FROM LoginActivity a
            WHERE a.userId = :userId
              AND a.status = :status
              AND a.createdAt >= :from
              AND a.createdAt <= :to
            GROUP BY a.browser
            ORDER BY COUNT(a) DESC
            """)
    List<BrowserCountDto> findBrowserCounts(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to);

    List<LoginActivity> findAllByUserId(Long userId);
}
