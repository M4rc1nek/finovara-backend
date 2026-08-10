package com.finovara.authservice.util.user;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.authservice.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserManagerService userManagerService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
    }

    @Nested
    class GetUserById {

        @Test
        void shouldReturnUserWhenIdExists() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            User result = userManagerService.getUserByIdOrThrow(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        void shouldThrowRequestedEntityNotFoundExceptionWhenIdDoesNotExist() {
            Long userId = 999L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> userManagerService.getUserByIdOrThrow(userId));
        }

        @Test
        void shouldReturnUserWithCorrectIdWhenUserExists() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            User result = userManagerService.getUserByIdOrThrow(1L);

            assertThat(result.getId()).isEqualTo(testUser.getId());
        }
    }

    @Nested
    class GetUserByEmail {

        @Test
        void shouldReturnUserWhenEmailExists() {
            String email = "test@example.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

            User result = userManagerService.getUserByEmailOrThrow(email);

            assertThat(result.getEmail()).isEqualTo(email);
        }

        @Test
        void shouldThrowRequestedEntityNotFoundExceptionWhenEmailDoesNotExist() {
            String email = "nonexistent@example.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> userManagerService.getUserByEmailOrThrow(email));
        }
    }
}