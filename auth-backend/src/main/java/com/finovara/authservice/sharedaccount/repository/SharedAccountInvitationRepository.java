package com.finovara.authservice.sharedaccount.repository;

import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.dto.InvitationResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedAccountInvitationRepository extends JpaRepository<SharedAccountInvitation, Long> {

    @Query("""
                SELECT i FROM SharedAccountInvitation i 
                WHERE (i.inviterUserId = :userAId AND i.inviteeUserId = :userBId) 
                   OR (i.inviterUserId = :userBId AND i.inviteeUserId = :userAId)
            """)
    Optional<SharedAccountInvitation> findInvitationBetweenUsers(Long userAId, Long userBId);

    @Query("SELECT i FROM SharedAccountInvitation i WHERE i.id = :invitationId")
    Optional<SharedAccountInvitation> findInvitationForInviteeUser(Long invitationId);

    @Query("""
                SELECT new com.finovara.authservice.sharedaccount.dto.InvitationResponse(
                    i.id, i.inviterUserId, u.username
                )
                FROM SharedAccountInvitation i
                JOIN User u ON u.id = i.inviterUserId
                WHERE i.inviteeUserId = :userId
            """)
    List<InvitationResponse> findInvitationWithInviterUsername(Long userId);

    @Query("""
                SELECT u.username
                FROM SharedAccountInvitation i
                JOIN User u ON u.id = i.inviteeUserId
                WHERE i.id = :invitationId
            """)
    Optional<String> findInviteeUsernameByInvitationId(Long invitationId);

}