package com.finovara.finovarabackend.accountactivity.login.archive.repository;

import com.finovara.finovarabackend.accountactivity.login.archive.model.ArchiveLoginActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchiveLoginActivityRepository extends JpaRepository<ArchiveLoginActivity, Long> {

    @Query("SELECT a FROM ArchiveLoginActivity a WHERE a.userAssigned.email = :email ORDER BY a.id DESC")
    List<ArchiveLoginActivity> findAllByUserAssignedEmailOrderByIdDesc(@Param("email") String email);


}
