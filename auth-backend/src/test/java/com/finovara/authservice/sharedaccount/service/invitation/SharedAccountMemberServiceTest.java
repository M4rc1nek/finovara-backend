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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedAccountMemberServiceTest {

    @Mock private SharedAccountMemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserDataMapper userDataMapper;
    @InjectMocks private SharedAccountMemberService service;
    private SharedAccount account;

    @BeforeEach
    void setUp() {
        account = SharedAccount.builder().id(10L).build();
    }

    @Nested
    class CreateOwnerAndMember {
        @Test
        void shouldCreateOwnerAndMemberAndMarkBothUsersAsSharedAccountMembers() {
            User owner = User.builder().id(1L).build();
            User member = User.builder().id(2L).build();
            when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(owner, member));

            service.createOwnerAndMember(account, 1L, 2L);

            ArgumentCaptor<List<SharedAccountMember>> captor = ArgumentCaptor.forClass(List.class);
            verify(memberRepository).saveAll(captor.capture());
            assertEquals(SharedRole.OWNER, captor.getValue().get(0).getRole());
            assertEquals(SharedRole.MEMBER, captor.getValue().get(1).getRole());
            assertEquals(account, captor.getValue().get(0).getSharedAccount());
            assertEquals(true, owner.isHasSharedAccount());
            assertEquals(true, member.isHasSharedAccount());
        }
    }

    @Nested
    class MemberDetails {
        @Test
        void shouldMapDetailsWhenUsersExistForEveryMember() {
            SharedAccountMember membership = SharedAccountMember.builder().userId(1L).role(SharedRole.OWNER).build();
            User user = User.builder().id(1L).username("anna").build();
            SharedAccountMemberDto dto = new SharedAccountMemberDto(1L, "anna", null, SharedRole.OWNER, null);
            when(memberRepository.findMembersByUserId(1L)).thenReturn(List.of(membership));
            when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(user));
            when(userDataMapper.toSharedAccountMemberDto(membership, user)).thenReturn(dto);

            assertEquals(List.of(dto), service.getMemberDetails(1L));
        }

        @Test
        void shouldThrowExceptionWhenMemberUserCannotBeLoaded() {
            SharedAccountMember membership = SharedAccountMember.builder().userId(1L).build();
            when(memberRepository.findMembersByUserId(1L)).thenReturn(List.of(membership));
            when(userRepository.findAllById(anyList())).thenReturn(List.of());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.getMemberDetails(1L));
        }
    }

    @Nested
    class MembersInfo {
        @Test
        void shouldReturnMemberInfoWhenUsersExist() {
            SharedAccountMember membership = SharedAccountMember.builder().userId(1L).role(SharedRole.OWNER).build();
            User user = User.builder().id(1L).username("anna").build();
            when(memberRepository.findMembersByUserId(1L)).thenReturn(List.of(membership));
            when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(user));

            List<SharedAccountMemberInfoDto> result = service.getMembersInfo(1L);

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).userId());
            assertEquals("anna", result.get(0).username());
        }
    }
}
