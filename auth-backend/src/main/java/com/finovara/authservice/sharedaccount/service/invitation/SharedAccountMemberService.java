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

    @Transactional
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

    @Transactional(readOnly = true)
    public List<SharedAccountMemberDto> getMemberDetails(Long callerId) {
        List<SharedAccountMember> members = loadAccountMembers(callerId);
        Map<Long, User> usersById = loadUsersById(members);

        return members.stream()
                .map(member -> userDataMapper.toSharedAccountMemberDto(member, usersById.get(member.getUserId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SharedAccountMemberInfoDto> getMembersInfo(Long userId) {
        List<SharedAccountMember> members = loadAccountMembers(userId);
        Map<Long, User> usersById = loadUsersById(members);

        return members.stream()
                .map(member -> {
                    User user = usersById.get(member.getUserId());
                    return new SharedAccountMemberInfoDto(user.getId(), user.getUsername(), member.getRole());
                })
                .toList();
    }

    private List<SharedAccountMember> loadAccountMembers(Long userId) {
        SharedAccountMember membership = sharedAccountMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("User is not a member of any shared account"));

        return sharedAccountMemberRepository.findMembersByAccountId(membership.getSharedAccount().getId());
    }

    private Map<Long, User> loadUsersById(List<SharedAccountMember> members) {
        Map<Long, User> usersById = userRepository
                .findAllById(members.stream().map(SharedAccountMember::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        members.forEach(member -> validateUserExists(usersById.get(member.getUserId()), member));
        return usersById;
    }

    private void validateUserExists(User user, SharedAccountMember member) {
        if (user == null) {
            throw new RequestedEntityNotFoundException("User not found, userId=" + member.getUserId());
        }
    }
}