package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.dto.SharedAccountMemberDto;
import com.finovara.authservice.sharedaccount.model.SharedAccount;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.sharedaccount.model.role.SharedRole;
import com.finovara.authservice.sharedaccount.repository.SharedAccountMemberRepository;
import com.finovara.authservice.user.mapper.UserDataMapper;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedAccountMemberServiceTest {

    @Mock
    private SharedAccountMemberRepository sharedAccountMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDataMapper userDataMapper;

    @Mock
    private InvitationValidator invitationValidator;

    private SharedAccountMemberService sharedAccountMemberService;

    private Long userId;

    private Long accountId;

    private SharedAccount sharedAccount;

    @BeforeEach
    void setUp() {
        sharedAccountMemberService = new SharedAccountMemberService(
                sharedAccountMemberRepository, userRepository, userDataMapper, invitationValidator);

        userId = 1L;
        accountId = 5L;
    }

    @Nested
    class CreateMember {

        @Test
        void shouldSaveMemberWithGivenRoleWhenCreatingMember() {
            User user = mock(User.class);
            when(userRepository.getReferenceById(userId)).thenReturn(user);

            sharedAccountMemberService.createMember(sharedAccount, userId, SharedRole.OWNER);

            ArgumentCaptor<SharedAccountMember> memberCaptor = ArgumentCaptor.forClass(SharedAccountMember.class);
            verify(sharedAccountMemberRepository).save(memberCaptor.capture());
            assertEquals(sharedAccount, memberCaptor.getValue().getSharedAccount());
            assertEquals(userId, memberCaptor.getValue().getUserId());
            assertEquals(SharedRole.OWNER, memberCaptor.getValue().getRole());
        }

        @Test
        void shouldMarkUserAsHavingSharedAccountWhenCreatingMember() {
            User user = mock(User.class);
            when(userRepository.getReferenceById(userId)).thenReturn(user);

            sharedAccountMemberService.createMember(sharedAccount, userId, SharedRole.MEMBER);

            verify(user).setHasSharedAccount(true);
        }

        @Test
        void shouldSaveMemberWithMemberRoleWhenCreatingRegularMember() {
            User user = mock(User.class);
            when(userRepository.getReferenceById(userId)).thenReturn(user);

            sharedAccountMemberService.createMember(sharedAccount, userId, SharedRole.MEMBER);

            ArgumentCaptor<SharedAccountMember> memberCaptor = ArgumentCaptor.forClass(SharedAccountMember.class);
            verify(sharedAccountMemberRepository).save(memberCaptor.capture());
            assertEquals(SharedRole.MEMBER, memberCaptor.getValue().getRole());
        }
    }

    @Nested
    class GetMemberDetails {

        @Test
        void shouldReturnMemberDetailsWhenCallerIsMember() {
            SharedAccountMember member = mock(SharedAccountMember.class);
            when(member.getUserId()).thenReturn(userId);
            when(sharedAccountMemberRepository.findMembersByAccountId(accountId)).thenReturn(List.of(member));

            User user = mock(User.class);
            when(userRepository.getReferenceById(userId)).thenReturn(user);

            SharedAccountMemberDto memberDto = mock(SharedAccountMemberDto.class);
            when(userDataMapper.toSharedAccountMemberDto(member, user)).thenReturn(memberDto);

            List<SharedAccountMemberDto> result = sharedAccountMemberService.getMemberDetails(accountId, userId);

            assertEquals(1, result.size());
            assertEquals(memberDto, result.get(0));
        }

        @Test
        void shouldReturnEmptyListWhenAccountHasNoMembers() {
            when(sharedAccountMemberRepository.findMembersByAccountId(accountId)).thenReturn(List.of());

            List<SharedAccountMemberDto> result = sharedAccountMemberService.getMemberDetails(accountId, userId);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldValidateMembershipBeforeFetchingMembers() {
            when(sharedAccountMemberRepository.findMembersByAccountId(accountId)).thenReturn(List.of());

            sharedAccountMemberService.getMemberDetails(accountId, userId);

            verify(invitationValidator, times(1)).validateMembership(accountId, userId);
        }

        @Test
        void shouldMapEachMemberUsingUserDataMapperWhenAccountHasMultipleMembers() {
            SharedAccountMember memberOne = mock(SharedAccountMember.class);
            when(memberOne.getUserId()).thenReturn(userId);
            SharedAccountMember memberTwo = mock(SharedAccountMember.class);
            when(memberTwo.getUserId()).thenReturn(2L);

            when(sharedAccountMemberRepository.findMembersByAccountId(accountId))
                    .thenReturn(List.of(memberOne, memberTwo));

            User userOne = mock(User.class);
            User userTwo = mock(User.class);
            when(userRepository.getReferenceById(userId)).thenReturn(userOne);
            when(userRepository.getReferenceById(2L)).thenReturn(userTwo);

            SharedAccountMemberDto dtoOne = mock(SharedAccountMemberDto.class);
            SharedAccountMemberDto dtoTwo = mock(SharedAccountMemberDto.class);
            when(userDataMapper.toSharedAccountMemberDto(memberOne, userOne)).thenReturn(dtoOne);
            when(userDataMapper.toSharedAccountMemberDto(memberTwo, userTwo)).thenReturn(dtoTwo);

            List<SharedAccountMemberDto> result = sharedAccountMemberService.getMemberDetails(accountId, userId);

            assertEquals(2, result.size());
            assertTrue(result.containsAll(List.of(dtoOne, dtoTwo)));
            verify(userDataMapper, times(2)).toSharedAccountMemberDto(any(SharedAccountMember.class), any(User.class));
        }
    }
}