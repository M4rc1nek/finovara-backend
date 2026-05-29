package com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.repository;

import com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.dto.LoginActivityArchiveDto;
import com.finovara.activityservice.activitylog.accountactivity.secure.login.archive.model.LoginActivityArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginActivityArchiveRepository extends JpaRepository<LoginActivityArchive, Long> {

    @Query("""
            SELECT new com.finovara.activityservice.activity_log.accountactivity.secure.login.archive.dto.LoginActivityArchiveDto(
           l.type, l.status, l.moveToArchiveDate, l.activityLoginDate, l.browser, l.ipAddress, l.location)
           FROM LoginActivityArchive l
           WHERE l.userId = :userId
           ORDER BY l.id DESC
            """)
    List<LoginActivityArchiveDto> findAllByUserIdOrderByIdDesc(@Param("userId") Long userId);

}
