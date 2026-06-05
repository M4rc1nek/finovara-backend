package com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.repository;

import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.dto.AccountChangesActivityDto;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.model.AccountChangesActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountChangesActivityRepository extends JpaRepository<AccountChangesActivity, Long> {

    @Query("""
            SELECT new com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.activity.dto.AccountChangesActivityDto(
            a.type, a.createdAt, a.browser, a.ipAddress, a.location) 
            FROM AccountChangesActivity a 
            WHERE a.userId = :userId
            ORDER BY a.id DESC
            """)
     List<AccountChangesActivityDto> findByUserIdOrderByIdDesc(Long userId);

    @Query("SELECT COUNT(u) FROM AccountChangesActivity u WHERE u.userId = :userId")
    long countAccountChangesByUserId(Long userId);

    @Query("SELECT u FROM AccountChangesActivity u WHERE u.userId = :userId ORDER BY u.id")
     List<AccountChangesActivity> findFewByUserId(Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}
