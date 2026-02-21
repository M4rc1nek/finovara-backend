package com.finovara.finovarabackend.accountactivity.login.archive.repository;

import com.finovara.finovarabackend.accountactivity.login.archive.model.LoginActivityArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginActivityArchiveRepository extends JpaRepository<LoginActivityArchive, Long> {

    @Query("SELECT a FROM LoginActivityArchive a WHERE a.userAssigned.email = :email ORDER BY a.id DESC")
    List<LoginActivityArchive> findAllByUserAssignedEmailOrderByIdDesc(@Param("email") String email);

}
