package com.finovara.finovarabackend.limit.management.delete;

import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivityType;
import com.finovara.finovarabackend.accountactivity.limit.service.LimitActivityService;
import com.finovara.finovarabackend.limit.exception.notfound.ActiveLimitNotFoundException;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.limit.service.LimitManagementService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteLimitTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private LimitRepository limitRepository;

    @Mock
    private LimitActivityService limitActivityService;

    @InjectMocks
    private LimitManagementService limitManagementService;

    @Test
    void shouldDeleteLimitSuccessfully() {
        String email = "user@example.com";
        Long limitId = 10L;

        User user = new User();
        user.setId(1L);

        Limit limit = new Limit();
        limit.setId(limitId);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(limitRepository.findByIdAndUserAssignedId(user.getId(), limitId)).thenReturn(java.util.Optional.of(limit));

        limitManagementService.deleteLimit(email, limitId);

        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(limitRepository).findByIdAndUserAssignedId(user.getId(), limitId);
        verify(limitActivityService).createLimitActivity(email, LimitActivityType.DELETED_LIMIT, limit);
        verify(limitRepository).delete(limit);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        String email = "missing@example.com";
        Long limitId = 10L;

        when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> limitManagementService.deleteLimit(email, limitId));

        verify(userManagerService).getUserByEmailOrThrow(email);
        verifyNoInteractions(limitRepository, limitActivityService);
        verify(limitRepository, never()).delete(any());
    }

    @Test
    void shouldThrowExceptionWhenLimitNotFound() {
        String email = "user@example.com";
        Long limitId = 10L;

        User user = new User();
        user.setId(1L);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(limitRepository.findByIdAndUserAssignedId(user.getId(), limitId)).thenReturn(java.util.Optional.empty());

      assertThrows(ActiveLimitNotFoundException.class,() -> limitManagementService.deleteLimit(email, limitId));

        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(limitRepository).findByIdAndUserAssignedId(user.getId(), limitId);
        verifyNoInteractions(limitActivityService);
        verify(limitRepository, never()).delete(any());
    }
}