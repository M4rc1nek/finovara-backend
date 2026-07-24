package com.finovara.authservice.sharedaccount.controller;

import com.finovara.authservice.security.SecurityUtils;
import com.finovara.authservice.sharedaccount.dto.InvitationResponse;
import com.finovara.authservice.sharedaccount.dto.SharedAccountMemberDto;
import com.finovara.authservice.sharedaccount.dto.SharedAccountStatusDto;
import com.finovara.authservice.sharedaccount.service.invitation.InvitationResponseService;
import com.finovara.authservice.sharedaccount.service.invitation.InvitationService;
import com.finovara.authservice.sharedaccount.service.invitation.SharedAccountMemberService;
import com.finovara.authservice.user.dto.UserDataDto;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shared-accounts/users")
@RequiredArgsConstructor
public class UserInvitationController {

    private final InvitationService invitationService;
    private final InvitationResponseService invitationResponseService;
    private final SharedAccountMemberService sharedAccountMemberService;

    @GetMapping("/search")
    public ResponseEntity<List<UserDataDto>> search(@RequestParam @Size(min = 3, max = 20, message = "Query must be between 3 and 20 characters")
                                                    String query) {
        return ResponseEntity.ok(invitationService.searchUser(query, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/invitations/{inviteeUserId}")
    public ResponseEntity<Void> createInvitation(@PathVariable Long inviteeUserId) {
        invitationService.sendInvitation(SecurityUtils.getCurrentUserId(), inviteeUserId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/invitations/pending")
    public ResponseEntity<List<InvitationResponse>> getPendingInvitations() {
        return ResponseEntity.ok(invitationService.getPendingInvitations(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<Void> acceptInvite(@PathVariable Long invitationId) {
        invitationResponseService.acceptInvite(SecurityUtils.getCurrentUserId(), invitationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitations/{invitationId}/reject")
    public ResponseEntity<Void> rejectInvite(@PathVariable Long invitationId) {
        invitationResponseService.rejectInvite(SecurityUtils.getCurrentUserId(), invitationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/members-details/{accountId}")
    public ResponseEntity<List<SharedAccountMemberDto>> getMembersDetails(@PathVariable Long accountId) {
        return ResponseEntity.ok(sharedAccountMemberService.getMemberDetails(accountId, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/status")
    public ResponseEntity<SharedAccountStatusDto> hasSharedAccount() {
        return ResponseEntity.ok(invitationService.hasSharedAccount(SecurityUtils.getCurrentUserId()));
    }
}