package com.finovara.finovarabackend.accountactivity.secure.login.activity.repository;

import com.finovara.finovarabackend.accountactivity.secure.login.activity.dto.LoginActivityDto;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.model.LoginActivity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginActivityRepository extends JpaRepository<LoginActivity, Long> {

    @Query("""
            SELECT new com.finovara.finovarabackend.accountactivity.secure.login.activity.dto.LoginActivityDto(
            l.type, l.status, l.date, l.browser, l.ipAddress, l.location)
            FROM LoginActivity l
            WHERE l.userAssigned.email = :email
            ORDER BY l.id DESC
            """)
    List<LoginActivityDto> findByUserAssignedEmailOrderByDesc(@Param("email") String email);

    @Query("SELECT u FROM LoginActivity u WHERE u.userAssigned.id = :userId ORDER BY u.id ASC")
    List<LoginActivity> findOldestByUserAssignedId(Long userId, PageRequest of);

    @Query("SELECT COUNT(u) FROM LoginActivity u WHERE u.userAssigned.id = :userId")
    long countActivityLoginByUserAssignedId(Long userId);

}
