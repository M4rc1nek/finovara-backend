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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedAccountMemberServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long ACCOUNT_ID = 5L;
    private static final String USERNAME = "john";
    private static final String OTHER_USERNAME = "anna";

    @Mock
    private SharedAccountMemberRepository sharedAccountMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDataMapper userDataMapper;

    @InjectMocks
    private SharedAccountMemberService sharedAccountMemberService;

    @Nested
    class CreateMember {

        private SharedAccount sharedAccount;

        private User user;

        @BeforeEach
        void setUp() {
            sharedAccount = mock(SharedAccount.class);
            user = mock(User.class);
        }

        @Test
        void shouldSaveMemberWithGivenDataWhenCreatingOwner() {
            when(userRepository.getReferenceById(USER_ID)).thenReturn(user);

            sharedAccountMemberService.createMember(sharedAccount, USER_ID, SharedRole.OWNER);

            ArgumentCaptor<SharedAccountMember> captor = ArgumentCaptor.forClass(SharedAccountMember.class);
            verify(sharedAccountMemberRepository).save(captor.capture());
            assertThat(captor.getValue().getSharedAccount()).isSameAs(sharedAccount);
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getRole()).isEqualTo(SharedRole.OWNER);
        }

        @Test
        void shouldSaveMemberWithMemberRoleWhenCreatingRegularMember() {
            when(userRepository.getReferenceById(USER_ID)).thenReturn(user);

            sharedAccountMemberService.createMember(sharedAccount, USER_ID, SharedRole.MEMBER);

            ArgumentCaptor<SharedAccountMember> captor = ArgumentCaptor.forClass(SharedAccountMember.class);
            verify(sharedAccountMemberRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(SharedRole.MEMBER);
        }

        @Test
        void shouldSetJoinedAtToCurrentTimeWhenCreatingMember() {
            when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
            LocalDateTime before = LocalDateTime.now();

            sharedAccountMemberService.createMember(sharedAccount, USER_ID, SharedRole.MEMBER);

            LocalDateTime after = LocalDateTime.now();
            ArgumentCaptor<SharedAccountMember> captor = ArgumentCaptor.forClass(SharedAccountMember.class);
            verify(sharedAccountMemberRepository).save(captor.capture());
            assertThat(captor.getValue().getJoinedAt()).isBetween(before, after);
        }

        @Test
        void shouldMarkUserAsHavingSharedAccountWhenCreatingMember() {
            when(userRepository.getReferenceById(USER_ID)).thenReturn(user);

            sharedAccountMemberService.createMember(sharedAccount, USER_ID, SharedRole.MEMBER);

            verify(user).setHasSharedAccount(true);
        }

        @Test
        void shouldSaveMemberWithNullRoleWhenRoleIsNull() {
            when(userRepository.getReferenceById(USER_ID)).thenReturn(user);

            sharedAccountMemberService.createMember(sharedAccount, USER_ID, null);

            ArgumentCaptor<SharedAccountMember> captor = ArgumentCaptor.forClass(SharedAccountMember.class);
            verify(sharedAccountMemberRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isNull();
        }

        @Test
        void shouldThrowExceptionWhenUserReferenceCannotBeLoaded() {
            when(userRepository.getReferenceById(USER_ID)).thenThrow(new IllegalStateException("db error"));

            assertThrows(IllegalStateException.class,
                    () -> sharedAccountMemberService.createMember(sharedAccount, USER_ID, SharedRole.MEMBER));
            verifyNoInteractions(sharedAccountMemberRepository);
        }

        @Test
        void shouldThrowExceptionWhenSavingMemberFails() {
            when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
            doThrow(new IllegalStateException("save failed"))
                    .when(sharedAccountMemberRepository)
                    .save(any(SharedAccountMember.class));

            assertThrows(IllegalStateException.class,
                    () -> sharedAccountMemberService.createMember(sharedAccount, USER_ID, SharedRole.MEMBER));
            verify(user, never()).setHasSharedAccount(anyBoolean());
        }
    }

    @Nested
    class GetMemberDetails {

        private SharedAccount sharedAccount;

        private SharedAccountMember membership;

        private SharedAccountMember member;

        private SharedAccountMember otherMember;

        private User user;

        private User otherUser;

        private SharedAccountMemberDto dto;

        private SharedAccountMemberDto otherDto;

        @BeforeEach
        void setUp() {
            sharedAccount = mock(SharedAccount.class);
            membership = mock(SharedAccountMember.class);
            member = mock(SharedAccountMember.class);
            otherMember = mock(SharedAccountMember.class);
            user = mock(User.class);
            otherUser = mock(User.class);
            dto = mock(SharedAccountMemberDto.class);
            otherDto = mock(SharedAccountMemberDto.class);
        }

        @Test
        void shouldReturnMemberDetailsWhenCallerIsMember() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of(member));
            when(member.getUserId()).thenReturn(USER_ID);
            when(userRepository.findAllById(List.of(USER_ID))).thenReturn(List.of(user));
            when(user.getId()).thenReturn(USER_ID);
            when(userDataMapper.toSharedAccountMemberDto(member, user)).thenReturn(dto);

            List<SharedAccountMemberDto> result = sharedAccountMemberService.getMemberDetails(USER_ID);

            assertThat(result).containsExactly(dto);
        }

        @Test
        void shouldMapEachMemberWithMatchingUserWhenAccountHasMultipleMembers() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of(member, otherMember));
            when(member.getUserId()).thenReturn(USER_ID);
            when(otherMember.getUserId()).thenReturn(OTHER_USER_ID);
            when(userRepository.findAllById(List.of(USER_ID, OTHER_USER_ID))).thenReturn(List.of(otherUser, user));
            when(user.getId()).thenReturn(USER_ID);
            when(otherUser.getId()).thenReturn(OTHER_USER_ID);
            when(userDataMapper.toSharedAccountMemberDto(member, user)).thenReturn(dto);
            when(userDataMapper.toSharedAccountMemberDto(otherMember, otherUser)).thenReturn(otherDto);

            List<SharedAccountMemberDto> result = sharedAccountMemberService.getMemberDetails(USER_ID);

            assertThat(result).containsExactly(dto, otherDto);
        }

        @Test
        void shouldQueryMembersUsingAccountIdFromCallerMembershipWhenCallerIsMember() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of());

            sharedAccountMemberService.getMemberDetails(USER_ID);

            verify(sharedAccountMemberRepository).findMembersByAccountId(ACCOUNT_ID);
        }

        @Test
        void shouldReturnEmptyListWhenAccountHasNoMembers() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of());

            List<SharedAccountMemberDto> result = sharedAccountMemberService.getMemberDetails(USER_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(userDataMapper);
        }

        @Test
        void shouldThrowExceptionWhenCallerIsNotMemberOfAnySharedAccount() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> sharedAccountMemberService.getMemberDetails(USER_ID));
            verify(sharedAccountMemberRepository, never()).findMembersByAccountId(any());
            verifyNoInteractions(userRepository, userDataMapper);
        }

        @Test
        void shouldThrowExceptionWhenCallerIdIsNull() {
            when(sharedAccountMemberRepository.findByUserId(null)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> sharedAccountMemberService.getMemberDetails(null));
            verifyNoInteractions(userRepository, userDataMapper);
        }

        @Test
        void shouldThrowExceptionWhenUserOfMemberDoesNotExist() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of(member));
            when(member.getUserId()).thenReturn(USER_ID);
            when(userRepository.findAllById(List.of(USER_ID))).thenReturn(List.of());

            assertThrows(RequestedEntityNotFoundException.class, () -> sharedAccountMemberService.getMemberDetails(USER_ID));
            verifyNoInteractions(userDataMapper);
        }

        @Test
        void shouldThrowExceptionWhenUserOfOneMemberDoesNotExist() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of(member, otherMember));
            when(member.getUserId()).thenReturn(USER_ID);
            when(otherMember.getUserId()).thenReturn(OTHER_USER_ID);
            when(userRepository.findAllById(List.of(USER_ID, OTHER_USER_ID))).thenReturn(List.of(user));
            when(user.getId()).thenReturn(USER_ID);

            assertThrows(RequestedEntityNotFoundException.class, () -> sharedAccountMemberService.getMemberDetails(USER_ID));
            verifyNoInteractions(userDataMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryReturnsDuplicatedUsers() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of(member));
            when(member.getUserId()).thenReturn(USER_ID);
            when(userRepository.findAllById(List.of(USER_ID))).thenReturn(List.of(user, user));
            when(user.getId()).thenReturn(USER_ID);

            assertThrows(IllegalStateException.class, () -> sharedAccountMemberService.getMemberDetails(USER_ID));
            verifyNoInteractions(userDataMapper);
        }

        @Test
        void shouldThrowExceptionWhenMappingMemberFails() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of(member));
            when(member.getUserId()).thenReturn(USER_ID);
            when(userRepository.findAllById(List.of(USER_ID))).thenReturn(List.of(user));
            when(user.getId()).thenReturn(USER_ID);
            when(userDataMapper.toSharedAccountMemberDto(member, user)).thenThrow(new IllegalStateException("mapping failed"));

            assertThrows(IllegalStateException.class, () -> sharedAccountMemberService.getMemberDetails(USER_ID));
        }
    }

    @Nested
    class GetMembersInfo {

        private SharedAccount sharedAccount;

        private SharedAccountMember membership;

        private SharedAccountMember member;

        private SharedAccountMember otherMember;

        private User user;

        private User otherUser;

        @BeforeEach
        void setUp() {
            sharedAccount = mock(SharedAccount.class);
            membership = mock(SharedAccountMember.class);
            member = mock(SharedAccountMember.class);
            otherMember = mock(SharedAccountMember.class);
            user = mock(User.class);
            otherUser = mock(User.class);
        }

        @Test
        void shouldReturnMemberInfoWhenCallerIsMember() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of(member));
            when(member.getUserId()).thenReturn(USER_ID);
            when(member.getRole()).thenReturn(SharedRole.OWNER);
            when(userRepository.findAllById(List.of(USER_ID))).thenReturn(List.of(user));
            when(user.getId()).thenReturn(USER_ID);
            when(user.getUsername()).thenReturn(USERNAME);

            List<SharedAccountMemberInfoDto> result = sharedAccountMemberService.getMembersInfo(USER_ID);

            assertThat(result)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(new SharedAccountMemberInfoDto(USER_ID, USERNAME, SharedRole.OWNER));
        }

        @Test
        void shouldReturnInfoForEachMemberInOrderWhenAccountHasMultipleMembers() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of(member, otherMember));
            when(member.getUserId()).thenReturn(USER_ID);
            when(member.getRole()).thenReturn(SharedRole.OWNER);
            when(otherMember.getUserId()).thenReturn(OTHER_USER_ID);
            when(otherMember.getRole()).thenReturn(SharedRole.MEMBER);
            when(userRepository.findAllById(List.of(USER_ID, OTHER_USER_ID))).thenReturn(List.of(otherUser, user));
            when(user.getId()).thenReturn(USER_ID);
            when(user.getUsername()).thenReturn(USERNAME);
            when(otherUser.getId()).thenReturn(OTHER_USER_ID);
            when(otherUser.getUsername()).thenReturn(OTHER_USERNAME);

            List<SharedAccountMemberInfoDto> result = sharedAccountMemberService.getMembersInfo(USER_ID);

            assertThat(result)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(
                            new SharedAccountMemberInfoDto(USER_ID, USERNAME, SharedRole.OWNER),
                            new SharedAccountMemberInfoDto(OTHER_USER_ID, OTHER_USERNAME, SharedRole.MEMBER));
        }

        @Test
        void shouldQueryMembersUsingAccountIdFromCallerMembershipWhenCallerIsMember() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of());

            sharedAccountMemberService.getMembersInfo(USER_ID);

            verify(sharedAccountMemberRepository).findMembersByAccountId(ACCOUNT_ID);
        }

        @Test
        void shouldReturnEmptyListWhenAccountHasNoMembers() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of());

            List<SharedAccountMemberInfoDto> result = sharedAccountMemberService.getMembersInfo(USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldThrowExceptionWhenCallerIsNotMemberOfAnySharedAccount() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> sharedAccountMemberService.getMembersInfo(USER_ID));
            verify(sharedAccountMemberRepository, never()).findMembersByAccountId(any());
            verifyNoInteractions(userRepository);
        }

        @Test
        void shouldThrowExceptionWhenUserIdIsNull() {
            when(sharedAccountMemberRepository.findByUserId(null)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> sharedAccountMemberService.getMembersInfo(null));
            verifyNoInteractions(userRepository);
        }

        @Test
        void shouldThrowExceptionWhenUserOfMemberDoesNotExist() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of(member));
            when(member.getUserId()).thenReturn(USER_ID);
            when(userRepository.findAllById(List.of(USER_ID))).thenReturn(List.of());

            assertThrows(RequestedEntityNotFoundException.class, () -> sharedAccountMemberService.getMembersInfo(USER_ID));
        }

        @Test
        void shouldThrowExceptionWhenUserOfOneMemberDoesNotExist() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of(member, otherMember));
            when(member.getUserId()).thenReturn(USER_ID);
            when(otherMember.getUserId()).thenReturn(OTHER_USER_ID);
            when(userRepository.findAllById(List.of(USER_ID, OTHER_USER_ID))).thenReturn(List.of(user));
            when(user.getId()).thenReturn(USER_ID);

            assertThrows(RequestedEntityNotFoundException.class, () -> sharedAccountMemberService.getMembersInfo(USER_ID));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryReturnsDuplicatedUsers() {
            when(sharedAccountMemberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(membership));
            when(membership.getSharedAccount()).thenReturn(sharedAccount);
            when(sharedAccount.getId()).thenReturn(ACCOUNT_ID);
            when(sharedAccountMemberRepository.findMembersByAccountId(ACCOUNT_ID)).thenReturn(List.of(member));
            when(member.getUserId()).thenReturn(USER_ID);
            when(userRepository.findAllById(List.of(USER_ID))).thenReturn(List.of(user, user));
            when(user.getId()).thenReturn(USER_ID);

            assertThrows(IllegalStateException.class, () -> sharedAccountMemberService.getMembersInfo(USER_ID));
        }
    }
}