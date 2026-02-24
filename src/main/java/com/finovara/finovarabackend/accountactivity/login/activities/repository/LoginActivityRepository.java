package com.finovara.finovarabackend.accountactivity.login.activities.repository;

import com.finovara.finovarabackend.accountactivity.login.activities.model.LoginActivity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginActivityRepository extends JpaRepository<LoginActivity, Long> {

    @Query("SELECT u FROM LoginActivity u WHERE u.userAssigned.email = :email ORDER BY u.id DESC")
    List<LoginActivity> findByUserAssignedEmailOrderByDesc(@Param("email") String email);

    @Query("SELECT u FROM LoginActivity u WHERE u.userAssigned.id = :userId ORDER BY u.id ASC")
    List<LoginActivity> findOldestByUserAssignedId(Long userId, PageRequest of);

    @Query("SELECT COUNT(u) FROM LoginActivity u WHERE u.userAssigned.id = :userId")
    long countActivityLoginByUserAssignedId(Long userId);

}
