package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.repository;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.dto.LoginActivityArchiveFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.archive.model.LoginActivityArchive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoginActivityArchiveRepository extends JpaRepository<LoginActivityArchive, Long> {

    List<LoginActivityArchive> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT a FROM LoginActivityArchive a
            WHERE a.userId = :userId
              AND (CAST(:#{#filter.status} AS string) IS NULL OR CAST(a.status AS string) = CAST(:#{#filter.status} AS string))
              AND (CAST(:#{#filter.browser} AS string) IS NULL OR :#{#filter.browser} = '' OR LOWER(a.browser) LIKE LOWER(CONCAT('%', :#{#filter.browser}, '%')))
              AND (CAST(:#{#filter.ipAddress} AS string) IS NULL OR :#{#filter.ipAddress} = '' OR LOWER(a.ipAddress) LIKE LOWER(CONCAT('%', :#{#filter.ipAddress}, '%')))
              AND (CAST(:#{#filter.location} AS string) IS NULL OR :#{#filter.location} = '' OR LOWER(a.location) LIKE LOWER(CONCAT('%', :#{#filter.location}, '%')))
              AND (CAST(:moveToArchiveDate AS date) IS NULL OR CAST(a.moveToArchiveDate AS date) = :moveToArchiveDate)
              AND (CAST(:createdAt AS date) IS NULL OR CAST(a.createdAt AS date) = :createdAt)
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<LoginActivityArchive> findFilteredActivities(Long userId, LoginActivityArchiveFilterDto filter, LocalDate moveToArchiveDate, LocalDate createdAt, Pageable pageable);

    List<LoginActivityArchive> findAllByUserId(Long userId);

}
