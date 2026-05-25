package com.finovara.corebackend.util.user;

import com.finovara.corebackend.user.exception.notfound.UserNotFoundException;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.user.repository.UserRepository;
import com.finovara.corebackend.util.user.service.UserManagerService;
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
}