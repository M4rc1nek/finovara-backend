package com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.repository;

import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.dto.AccountChangesActivityDto;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.model.AccountChangesActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountChangesActivityRepository extends JpaRepository<AccountChangesActivity, Long> {

    @Query("""
            SELECT new com.finovara.activityservice.activity_log.accountactivity.secure.accountchange.activity.dto.AccountChangesActivityDto(
            a.type, a.createdAt, a.browser, a.ipAddress, a.location) 
            FROM AccountChangesActivity a 
            WHERE a.userId = :userId
            ORDER BY a.id DESC
            """)
    List<AccountChangesActivityDto> findByUserIdOrderByIdDesc(@Param("userId") Long userId);

    @Query("SELECT COUNT(u) FROM AccountChangesActivity u WHERE u.userId = :userId")
    long countAccountChangesByUserId(@Param("userId") Long userId);

    @Query("SELECT u FROM AccountChangesActivity u WHERE u.userId = :userId ORDER BY u.id")
    List<AccountChangesActivity> findFewByUserId(@Param("userId") Long userId, Pageable pageable);
}
