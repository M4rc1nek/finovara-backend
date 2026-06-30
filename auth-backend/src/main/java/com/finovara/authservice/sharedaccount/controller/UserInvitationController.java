package com.finovara.authservice.sharedaccount.controller;

import com.finovara.authservice.security.SecurityUtils;
import com.finovara.authservice.sharedaccount.dto.SendInvitationRequest;
import com.finovara.authservice.sharedaccount.model.dto.InvitationResponse;
import com.finovara.authservice.sharedaccount.service.InvitationService;
import com.finovara.authservice.user.dto.UserDataDto;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shared-account/users")
@RequiredArgsConstructor
public class UserInvitationController {

    private final InvitationService invitationService;

    @GetMapping("/search")
    public ResponseEntity<List<UserDataDto>> search(@RequestParam @Size(min = 3, max = 20, message = "Query must be between 3 and 20 characters")
                                                        String query) {
        return ResponseEntity.ok(invitationService.searchUser(query));
    }

    @PostMapping("/send-invitation")
    public ResponseEntity<Void> createInvitation(@RequestBody SendInvitationRequest request) {
        invitationService.sendInvitation(SecurityUtils.getCurrentUserId(), request.inviteeUserId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/invitations/pending")
    public ResponseEntity<List<InvitationResponse>> getPendingInvitations() {
        return ResponseEntity.ok(invitationService.getPendingInvitations(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<Void> acceptInvite(@PathVariable Long invitationId) {
        invitationService.acceptInvite(SecurityUtils.getCurrentUserId(), invitationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitations/{invitationId}/reject")
    public ResponseEntity<Void> rejectInvite(@PathVariable Long invitationId) {
        invitationService.rejectInvite(SecurityUtils.getCurrentUserId(), invitationId);
        return ResponseEntity.noContent().build();
    }
}