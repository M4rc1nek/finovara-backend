package com.finovara.securitymonitoring.login.repository;

import com.finovara.securitymonitoring.login.model.LoginProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginProfileRepository extends JpaRepository<LoginProfile, Long> {

    boolean existsByUserId(Long userId);

    @Query("SELECT lp FROM LoginProfile lp WHERE lp.userId = :userId")
    Optional<LoginProfile> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
