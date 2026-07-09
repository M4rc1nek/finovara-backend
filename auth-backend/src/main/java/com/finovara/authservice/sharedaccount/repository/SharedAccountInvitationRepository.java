package com.finovara.authservice.sharedaccount.repository;

import com.finovara.authservice.sharedaccount.dto.InvitationDetailsDto;
import com.finovara.authservice.sharedaccount.dto.InvitationResponse;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("""
                SELECT new com.finovara.authservice.sharedaccount.dto.InvitationDetailsDto(
                    i.id,
                    i.inviterUserId,
                    inviter.username,
                    inviter.email,
                    i.inviteeUserId,
                    invitee.username
                )
                FROM SharedAccountInvitation i
                JOIN User inviter ON inviter.id = i.inviterUserId
                JOIN User invitee ON invitee.id = i.inviteeUserId
                WHERE i.id = :invitationId
            """)
    Optional<InvitationDetailsDto> findInvitationDetailsById(Long invitationId);

    @Query("""
                SELECT new com.finovara.authservice.sharedaccount.dto.InvitationResponse(
                    i.id, i.inviterUserId, u.username
                )
                FROM SharedAccountInvitation i
                JOIN User u ON u.id = i.inviterUserId
                WHERE i.inviteeUserId = :userId
            """)
    List<InvitationResponse> findInvitationWithInviterUsername(Long userId);

    @Modifying
    @Query("DELETE FROM SharedAccountInvitation i WHERE i.id = :invitationId")
    void deleteInvitationById(Long invitationId);
}