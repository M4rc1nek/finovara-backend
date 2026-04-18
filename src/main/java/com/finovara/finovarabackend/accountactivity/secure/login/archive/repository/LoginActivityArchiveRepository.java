package com.finovara.finovarabackend.accountactivity.secure.login.archive.repository;

import com.finovara.finovarabackend.accountactivity.secure.login.archive.dto.LoginActivityArchiveDto;
import com.finovara.finovarabackend.accountactivity.secure.login.archive.model.LoginActivityArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginActivityArchiveRepository extends JpaRepository<LoginActivityArchive, Long> {

    @Query("""
           SELECT new com.finovara.finovarabackend.accountactivity.secure.login.archive.dto.LoginActivityArchiveDto(
           l.type, l.status, l.moveToArchiveDate, l.activityLoginDate, l.browser, l.ipAddress, l.location)
           FROM LoginActivityArchive l
           WHERE l.userAssigned.id = :userId
           ORDER BY l.id DESC
            """)
    List<LoginActivityArchiveDto> findAllByUserAssignedIdOrderByIdDesc(@Param("userId") Long userId);

    @Query("""
           SELECT new com.finovara.finovarabackend.accountactivity.secure.login.archive.dto.LoginActivityArchiveDto(
           l.type, l.status, l.moveToArchiveDate, l.activityLoginDate, l.browser, l.ipAddress, l.location)
           FROM LoginActivityArchive l
           WHERE l.userAssigned.email = :email
           ORDER BY l.id DESC
            """)
    List<LoginActivityArchiveDto> findAllByUserAssignedEmailOrderByIdDesc(@Param("email") String email);

}
