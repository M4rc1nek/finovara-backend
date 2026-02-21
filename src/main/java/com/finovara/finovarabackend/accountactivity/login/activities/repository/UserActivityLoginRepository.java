package com.finovara.finovarabackend.accountactivity.login.activities.repository;

import com.finovara.finovarabackend.accountactivity.login.activities.model.UserActivityLogin;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActivityLoginRepository extends JpaRepository<UserActivityLogin, Long> {

    @Query("SELECT u FROM UserActivityLogin u WHERE u.userAssigned.email = :email ORDER BY u.id DESC")
    List<UserActivityLogin> findByUserAssignedEmailOrderByIdDesc(@Param("email") String email);

    @Query("SELECT u FROM UserActivityLogin u WHERE u.userAssigned.id = :userId ORDER BY u.id ASC")
    List<UserActivityLogin> findOldestByUserAssignedId(Long userId, PageRequest of);

    @Query("SELECT COUNT(u) FROM UserActivityLogin u WHERE u.userAssigned.id = :userId")
    long countActivityLoginByUserAssignedId(Long userId);

}
