package com.finovara.authservice.util.user.service;

import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.auth.dto.UserDataResponse;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagerServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserManagerService userManagerService;

    private Long userId;
    private String email;
    private String username;
    private User user;

    @BeforeEach
    void setUp() {
        userId = 1L;
        email = "john@example.com";
        username = "john_doe";
        user = new User();
    }

    @Nested
    class GetUserData {

        @Test
        void shouldReturnUserDataWhenUserExists() {
            when(userRepository.findEmailById(userId)).thenReturn(Optional.of(email));
            when(userRepository.findUsernameById(userId)).thenReturn(Optional.of(username));

            UserDataResponse response = userManagerService.getUserData(userId);

            assertEquals(userId, response.userId());
            assertEquals(Optional.of(username), response.username());
            assertEquals(Optional.of(email), response.email());
            verify(userRepository).findEmailById(userId);
            verify(userRepository).findUsernameById(userId);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void shouldReturnEmptyOptionalsWhenUserDataIsMissing() {
            when(userRepository.findEmailById(userId)).thenReturn(Optional.empty());
            when(userRepository.findUsernameById(userId)).thenReturn(Optional.empty());

            UserDataResponse response = userManagerService.getUserData(userId);

            assertEquals(userId, response.userId());
            assertEquals(Optional.empty(), response.username());
            assertEquals(Optional.empty(), response.email());
        }
    }

    @Nested
    class GetUserByIdOrThrow {

        @Test
        void shouldReturnUserWhenUserExists() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            User result = userManagerService.getUserByIdOrThrow(userId);

            assertEquals(user, result);
            verify(userRepository).findById(userId);
        }

        @Test
        void shouldThrowExceptionWhenUserIsMissing() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> userManagerService.getUserByIdOrThrow(userId));

            verify(userRepository).findById(userId);
        }
    }

    @Nested
    class GetUserByEmailOrThrow {

        @Test
        void shouldReturnUserWhenEmailExists() {
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            User result = userManagerService.getUserByEmailOrThrow(email);

            assertEquals(user, result);
            verify(userRepository).findByEmail(email);
        }

        @Test
        void shouldThrowExceptionWhenEmailIsMissing() {
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> userManagerService.getUserByEmailOrThrow(email));

            verify(userRepository).findByEmail(email);
        }
    }

    @Nested
    class GetUsernameByIdOrThrow {

        @Test
        void shouldReturnUsernameWhenValueExists() {
            when(userRepository.findUsernameById(userId)).thenReturn(Optional.of(username));

            String result = userManagerService.getUsernameByIdOrThrow(userId);

            assertEquals(username, result);
            verify(userRepository).findUsernameById(userId);
        }

        @Test
        void shouldThrowExceptionWhenUsernameIsMissing() {
            when(userRepository.findUsernameById(userId)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> userManagerService.getUsernameByIdOrThrow(userId));

            verify(userRepository).findUsernameById(userId);
        }
    }

    @Nested
    class GetUserEmailById {

        @Test
        void shouldReturnEmailWhenValueExists() {
            when(userRepository.findEmailById(userId)).thenReturn(Optional.of(email));

            String result = userManagerService.getUserEmailById(userId);

            assertEquals(email, result);
            verify(userRepository).findEmailById(userId);
        }

        @Test
        void shouldThrowExceptionWhenEmailIsMissing() {
            when(userRepository.findEmailById(userId)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> userManagerService.getUserEmailById(userId));

            verify(userRepository).findEmailById(userId);
        }
    }
}