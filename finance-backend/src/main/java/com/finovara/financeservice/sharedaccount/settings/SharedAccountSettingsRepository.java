package com.finovara.financeservice.sharedaccount.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedAccountSettingsRepository extends JpaRepository<SharedAccountSettings, Long> {

    @Query("SELECT sa FROM SharedAccountSettings sa WHERE sa.ownerId = :userId OR sa.memberId = :userId")
    Optional<SharedAccountSettings> findByOwnerIdOrMemberId(Long userId);

    @Query("SELECT e FROM SharedAccountSettings e WHERE e.ownerId = :ownerId AND e.memberId = :memberId")
    List<SharedAccountSettings> findAllByOwnerIdAndMemberId(Long ownerId, Long memberId);

}
