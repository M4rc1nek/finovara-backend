package com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.repository;

import com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.dto.AccountChangeArchiveDto;
import com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.model.AccountChangeArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountChangeArchiveRepository extends JpaRepository<AccountChangeArchive, Long> {

    @Query("""
            SELECT new com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.dto.AccountChangeArchiveDto(
            a.type, a.moveToArchiveDate, a.activityAccountChangesDate, a.browser, a.ipAddress, a.location)
            FROM AccountChangeArchive a
            WHERE a.userAssigned.id = :userId
            ORDER BY a.id DESC
            """)
    List<AccountChangeArchiveDto> findAllByUserAssignedIdOrderByIdDesc(@Param("userId") Long userId);

    @Query("""
            SELECT new com.finovara.finovarabackend.accountactivity.secure.accountchange.archive.dto.AccountChangeArchiveDto(
            a.type, a.moveToArchiveDate, a.activityAccountChangesDate, a.browser, a.ipAddress, a.location)
            FROM AccountChangeArchive a
            WHERE a.userAssigned.email = :email
            ORDER BY a.id DESC
            """)
    List<AccountChangeArchiveDto> findAllByUserAssignedEmailOrderByIdDesc(@Param("email") String email);
}
