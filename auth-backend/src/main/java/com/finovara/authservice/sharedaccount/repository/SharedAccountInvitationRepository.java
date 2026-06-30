package com.finovara.authservice.sharedaccount.repository;

import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.model.dto.InvitationResponse;
import com.finovara.authservice.sharedaccount.model.status.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedAccountInvitationRepository extends JpaRepository<SharedAccountInvitation, Long> {

    @Query("SELECT i FROM SharedAccountInvitation i WHERE i.inviteeUserId = :userId AND LOWER(CAST(i.status AS string)) = LOWER(CAST(:status AS string))")
    List<SharedAccountInvitation> findByInviteeUserIdAndStatus(Long userId, InvitationStatus status);

    @Query("SELECT i FROM SharedAccountInvitation i WHERE i.inviterUserId = :userId AND i.status = :status")
    List<SharedAccountInvitation> findByInviterUserIdAndStatus(Long userId, InvitationStatus status);

    @Query("""
                SELECT i FROM SharedAccountInvitation i
                WHERE i.status = 'PENDING'
                AND (
                    (i.inviterUserId = :userAId AND i.inviteeUserId = :userBId)
                    OR
                    (i.inviterUserId = :userBId AND i.inviteeUserId = :userAId)
                )
            """)
    Optional<SharedAccountInvitation> findPendingBetweenUsers(Long userAId, Long userBId);

    @Query("SELECT i FROM SharedAccountInvitation i WHERE i.id = :invitationId AND i.status = 'PENDING'")
    Optional<SharedAccountInvitation> findInvitationForInviteeUser(Long invitationId);

    @Query("""
                SELECT new com.finovara.authservice.sharedaccount.model.dto.InvitationResponse(
                    i.id, i.inviterUserId, u.username, i.status
                )
                FROM SharedAccountInvitation i
                JOIN User u ON u.id = i.inviterUserId
                WHERE i.inviteeUserId = :userId AND i.status = :status
            """)
    List<InvitationResponse> findPendingWithInviterUsername(Long userId, InvitationStatus status);

    @Query("""
                SELECT u.username
                FROM SharedAccountInvitation i
                JOIN User u ON u.id = i.inviteeUserId
                WHERE i.id = :invitationId
            """)
    Optional<String> findInviteeUsernameByInvitationId(Long invitationId);

}