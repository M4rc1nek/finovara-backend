package com.finovara.authservice.sharedaccount.repository;

import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedAccountInvitationRepository  extends JpaRepository<SharedAccountInvitation, Long> {
}
