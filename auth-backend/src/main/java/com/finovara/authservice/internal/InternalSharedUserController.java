package com.finovara.authservice.internal;

import com.finovara.authservice.sharedaccount.service.invitation.SharedAccountMemberService;
import com.finovara.contracts.sharedaccount.SharedAccountMemberInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/shared-account")
public class InternalSharedUserController {

    private final SharedAccountMemberService sharedAccountMemberService;

    @GetMapping("/members")
    public ResponseEntity<List<SharedAccountMemberInfoDto>> getSharedAccountMembers(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(sharedAccountMemberService.getMembersInfo(userId));
    }
}
