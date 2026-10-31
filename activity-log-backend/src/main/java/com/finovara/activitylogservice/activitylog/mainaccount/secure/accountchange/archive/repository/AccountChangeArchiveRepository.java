package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.dto.AccountChangeArchiveFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.archive.model.AccountChangeArchive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccountChangeArchiveRepository extends JpaRepository<AccountChangeArchive, Long> {

    List<AccountChangeArchive> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT a FROM AccountChangeArchive a
            WHERE a.userId = :userId
              AND (CAST(:#{#filter.type} AS string) IS NULL OR a.type = :#{#filter.type})
              AND (CAST(:#{#filter.browser} AS string) IS NULL OR :#{#filter.browser} = '' OR LOWER(a.browser) LIKE LOWER(CONCAT('%', :#{#filter.browser}, '%')))
              AND (CAST(:#{#filter.ipAddress} AS string) IS NULL OR :#{#filter.ipAddress} = '' OR LOWER(a.ipAddress) LIKE LOWER(CONCAT('%', :#{#filter.ipAddress}, '%')))
              AND (CAST(:#{#filter.location} AS string) IS NULL OR :#{#filter.location} = '' OR LOWER(a.location) LIKE LOWER(CONCAT('%', :#{#filter.location}, '%')))
              AND (CAST(:moveToArchiveFrom AS timestamp) IS NULL OR a.moveToArchiveDate >= :moveToArchiveFrom)
              AND (CAST(:moveToArchiveTo AS timestamp) IS NULL OR a.moveToArchiveDate < :moveToArchiveTo)
              AND (CAST(:createdFrom AS timestamp) IS NULL OR a.createdAt >= :createdFrom)
              AND (CAST(:createdTo AS timestamp) IS NULL OR a.createdAt < :createdTo)
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<AccountChangeArchive> findFilteredActivities( Long userId, AccountChangeArchiveFilterDto filter, LocalDateTime moveToArchiveFrom, LocalDateTime moveToArchiveTo, LocalDateTime createdFrom, LocalDateTime createdTo,
                                                      Pageable pageable);

    List<AccountChangeArchive> findAllByUserId(Long userId);
}
