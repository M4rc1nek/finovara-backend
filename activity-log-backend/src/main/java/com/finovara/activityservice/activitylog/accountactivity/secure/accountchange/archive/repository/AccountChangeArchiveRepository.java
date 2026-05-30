package com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.archive.repository;

import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.archive.dto.AccountChangeArchiveDto;
import com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.archive.model.AccountChangeArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountChangeArchiveRepository extends JpaRepository<AccountChangeArchive, Long> {

    @Query("""
            SELECT new com.finovara.activityservice.activitylog.accountactivity.secure.accountchange.archive.dto.AccountChangeArchiveDto(
            a.type, a.moveToArchiveDate, a.activityAccountChangesDate, a.browser, a.ipAddress, a.location)
            FROM AccountChangeArchive a
            WHERE a.userId = :userId
            ORDER BY a.id DESC
            """)
     List<AccountChangeArchiveDto> findAllByUserIdOrderByIdDesc(Long userId);
}
