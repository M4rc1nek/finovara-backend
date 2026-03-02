package com.finovara.finovarabackend.accountactivity.accountchange.activities.repository;

import com.finovara.finovarabackend.accountactivity.accountchange.activities.dto.AccountChangesActivityDto;
import com.finovara.finovarabackend.accountactivity.accountchange.activities.model.AccountChangesActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountChangesActivityRepository extends JpaRepository<AccountChangesActivity, Long> {

    @Query("""
            SELECT new com.finovara.finovarabackend.accountactivity.accountchange.activities.dto.AccountChangesActivityDto(
            a.type, a.date, a.browser, a.ipAddress, a.location) 
            FROM AccountChangesActivity a 
            WHERE a.userAssigned.email = :email 
            ORDER BY a.id DESC
            """)
    List<AccountChangesActivityDto> findByUserAssignedEmailOrderByIdDesc(@Param("email") String email);

    @Query("SELECT COUNT(u) FROM AccountChangesActivity u WHERE u.userAssigned.id = :userId")
    long countAccountChangesByUserAssignedId(Long userId);

    @Query("SELECT u FROM AccountChangesActivity u WHERE u.userAssigned.id = :userId ORDER BY u.id  ")
    List<AccountChangesActivity> findFewByUserAssignedId(Long userId, Pageable pageable);
}
