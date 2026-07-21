package com.finovara.financeservice.sharedaccount.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SharedAccountSettingsRepository extends JpaRepository<SharedAccountSettings, Long> {

    @Query("SELECT sa FROM SharedAccountSettings sa WHERE sa.ownerId = :userId OR sa.memberId = :userId")
    SharedAccountSettings findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM SharedAccountSettings sa WHERE sa.ownerId = :ownerId AND sa.memberId = :memberId")
    void deleteByOwnerIdAndMemberId(Long ownerId, Long memberId);

}
