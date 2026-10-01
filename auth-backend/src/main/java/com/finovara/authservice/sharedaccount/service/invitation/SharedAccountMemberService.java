package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.dto.SharedAccountMemberDto;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.user.mapper.UserDataMapper;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.sharedaccount.SharedAccountMemberInfoDto;
import com.finovara.contracts.sharedaccount.SharedRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public List<SharedAccountMemberInfoDto> getMembersInfo(Long userId) {
        SharedAccountMember membership = sharedAccountMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User is not a member of any shared account"));

        List<SharedAccountMember> members = sharedAccountMemberRepository.findMembersByAccountId(membership.getSharedAccount().getId());

        Map<Long, User> usersById = userRepository
                .findAllById(members.stream().map(SharedAccountMember::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return members.stream()
                .map(member -> {
                    User user = usersById.get(member.getUserId());
                    validateUserExists(user, member);
                    return new SharedAccountMemberInfoDto(user.getId(), user.getUsername(), member.getRole());
                })
                .toList();
    }

    private void validateUserExists(User user, SharedAccountMember member){
        if (user == null) {
            throw new RequestedEntityNotFoundException("User not found, userId=" + member.getUserId());
        }
    }
}