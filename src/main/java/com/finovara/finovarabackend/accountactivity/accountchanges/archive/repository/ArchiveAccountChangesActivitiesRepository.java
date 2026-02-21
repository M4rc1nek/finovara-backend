package com.finovara.finovarabackend.accountactivity.accountchanges.archive.repository;

import com.finovara.finovarabackend.accountactivity.accountchanges.archive.model.ArchiveAccountChangesActivities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchiveAccountChangesActivitiesRepository extends JpaRepository<ArchiveAccountChangesActivities, Long> {

    @Query("SELECT a FROM ArchiveAccountChangesActivities a WHERE a.userAssigned.email = :email ORDER BY a.id DESC")
    List<ArchiveAccountChangesActivities> findAllByUserAssignedEmailOrderByIdDesc(@Param("email") String email);
}
