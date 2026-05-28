package com.finovara.corebackend.util.user.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagerServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserManagerService userManagerService;

    @Test
    void shouldReturnUserByIdWhenExists() {
        Long userId = 1L;

        User user = new User();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userManagerService.getUserByIdOrThrow(userId);

        assertEquals(user, result);
        verify(userRepository).findById(userId);
    }

    @Test
    void shouldThrowWhenUserByIdNotFound() {
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(RequestedEntityNotFoundException.class, () -> userManagerService.getUserByIdOrThrow(userId));

        verify(userRepository).findById(userId);
    }

    @Test
    void shouldReturnUserByEmailWhenExists() {
        String email = "test@test.com";

        User user = new User();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        User result = userManagerService.getUserByEmailOrThrow(email);

        assertEquals(user, result);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void shouldThrowWhenUserByEmailNotFound() {
        String email = "test@test.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(RequestedEntityNotFoundException.class, () -> userManagerService.getUserByEmailOrThrow(email));

        verify(userRepository).findByEmail(email);
    }
}