package com.finovara.finovarabackend.accountactivity.accountchanges.activities.repository;

import com.finovara.finovarabackend.accountactivity.accountchanges.activities.model.AccountChangesActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountChangesActivityRepository extends JpaRepository<AccountChangesActivity, Long> {

    @Query("SELECT u FROM AccountChangesActivity u WHERE u.userAssigned.email = :email ORDER BY u.id DESC")
    List<AccountChangesActivity> findByUserAssignedEmailOrderByIdDesc(@Param("email") String email);

    @Query("SELECT COUNT(u) FROM AccountChangesActivity u WHERE u.userAssigned.id = :userId")
    long countAccountChangesByUserAssignedId(Long userId);

    @Query("SELECT u FROM AccountChangesActivity u WHERE u.userAssigned.id = :userId ORDER BY u.id  ")
    List<AccountChangesActivity> findFewByUserAssignedId(Long userId, Pageable pageable);
}
