package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.dto.LoginActivityArchiveFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.model.LoginActivityArchive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginActivityArchiveRepository extends JpaRepository<LoginActivityArchive, Long> {

    List<LoginActivityArchive> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT a FROM LoginActivityArchive a
            WHERE a.userId = :userId
              AND (CAST(:#{#filter.type} AS string) IS NULL OR :#{#filter.type} = '' OR LOWER(CAST(a.type AS string)) LIKE LOWER(CONCAT('%', CAST(:#{#filter.type} AS string), '%')))
              AND (CAST(:#{#filter.status} AS string) IS NULL OR CAST(a.status AS string) = CAST(:#{#filter.status} AS string))
              AND (CAST(:#{#filter.browser} AS string) IS NULL OR :#{#filter.browser} = '' OR LOWER(a.browser) LIKE LOWER(CONCAT('%', :#{#filter.browser}, '%')))
              AND (CAST(:#{#filter.ipAddress} AS string) IS NULL OR :#{#filter.ipAddress} = '' OR LOWER(a.ipAddress) LIKE LOWER(CONCAT('%', :#{#filter.ipAddress}, '%')))
              AND (CAST(:#{#filter.location} AS string) IS NULL OR :#{#filter.location} = '' OR LOWER(a.location) LIKE LOWER(CONCAT('%', :#{#filter.location}, '%')))
              AND (CAST(:moveToArchiveFrom AS timestamp) IS NULL OR a.moveToArchiveDate >= :moveToArchiveFrom)
              AND (CAST(:moveToArchiveTo AS timestamp) IS NULL OR a.moveToArchiveDate < :moveToArchiveTo)
              AND (CAST(:createdFrom AS timestamp) IS NULL OR a.createdAt >= :createdFrom)
              AND (CAST(:createdTo AS timestamp) IS NULL OR a.createdAt < :createdTo)
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<LoginActivityArchive> findFilteredActivities( Long userId, LoginActivityArchiveFilterDto filter, LocalDateTime moveToArchiveFrom, LocalDateTime moveToArchiveTo, LocalDateTime createdFrom, LocalDateTime createdTo,
                                                      Pageable pageable);

    List<LoginActivityArchive> findAllByUserId(Long userId);

}
