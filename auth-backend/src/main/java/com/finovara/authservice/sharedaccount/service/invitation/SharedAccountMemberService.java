package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.dto.SharedAccountMemberDto;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.model.role.SharedRole;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.user.mapper.UserDataMapper;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SharedAccountMemberService {

    private final SharedAccountMemberRepository sharedAccountMemberRepository;
    private final UserRepository userRepository;
    private final UserDataMapper userDataMapper;
    private final InvitationValidator invitationValidator;

    public void createMember(SharedAccount sharedAccount, Long userId, SharedRole role) {
        User user = userRepository.getReferenceById(userId);
        sharedAccountMemberRepository.save(
                SharedAccountMember.builder()
                        .sharedAccount(sharedAccount)
                        .userId(userId)
                        .role(role)
                        .joinedAt(LocalDateTime.now())
                        .build());
        user.setHasSharedAccount(true);
    }

    public List<SharedAccountMemberDto> getMemberDetails(Long accountId, Long callerId) {
        invitationValidator.validateMembership(accountId, callerId);

        return sharedAccountMemberRepository.findMembersByAccountId(accountId).stream()
                .map(member -> userDataMapper.toSharedAccountMemberDto(
                        member, userRepository.getReferenceById(member.getUserId())))
                .toList();
    }
}