package com.finovara.authservice.sharedaccount.controller;

import com.finovara.authservice.sharedaccount.service.InvitationService;
import com.finovara.authservice.user.dto.UserDataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shared-account/users")
@RequiredArgsConstructor
public class UserSearchController {

    private final InvitationService invitationService;

    @GetMapping("/search")
    public ResponseEntity<List<UserDataDto>> search(@RequestParam String query) {
        return ResponseEntity.ok(invitationService.searchUser(query));
    }
}