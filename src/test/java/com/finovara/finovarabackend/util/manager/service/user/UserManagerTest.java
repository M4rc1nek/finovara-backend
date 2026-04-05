package com.finovara.finovarabackend.util.manager.service.user;

import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserManagerService userManagerService;

    @Test
    void shouldReturnUserWhenIdExists() {
        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userManagerService.getUserByIdOrThrow(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenIdDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userManagerService.getUserByIdOrThrow(1L));
    }

    @Test
    void shouldReturnUserWhenEmailExists() {
        User user = new User();
        user.setId(1L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        User result = userManagerService.getUserByEmailOrThrow("test@example.com");

        assertEquals(1L, result.getId());
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userManagerService.getUserByEmailOrThrow("test@example.com"));
    }
}