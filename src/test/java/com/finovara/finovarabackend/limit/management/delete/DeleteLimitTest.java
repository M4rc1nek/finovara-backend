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
        Long userId = 1L;
        Long limitId = 10L;

        User user = new User();
        user.setId(userId);

        Limit limit = new Limit();
        limit.setId(limitId);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(limitRepository.findByIdAndUserAssignedId(user.getId(), limitId)).thenReturn(java.util.Optional.of(limit));

        limitManagementService.deleteLimit(userId, limitId);

        verify(userManagerService).getUserByIdOrThrow(userId);
        verify(limitRepository).findByIdAndUserAssignedId(user.getId(), limitId);
        verify(limitActivityService).createLimitActivity(userId, LimitActivityType.DELETED_LIMIT, limit);
        verify(limitRepository).delete(limit);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        Long userId = 1L;
        Long limitId = 10L;

        when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> limitManagementService.deleteLimit(userId, limitId));

        verify(userManagerService).getUserByIdOrThrow(userId);
        verifyNoInteractions(limitRepository, limitActivityService);
        verify(limitRepository, never()).delete(any());
    }

    @Test
    void shouldThrowExceptionWhenLimitNotFound() {
        Long userId = 1L;
        Long limitId = 10L;

        User user = new User();
        user.setId(userId);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(limitRepository.findByIdAndUserAssignedId(user.getId(), limitId)).thenReturn(java.util.Optional.empty());

      assertThrows(ActiveLimitNotFoundException.class,() -> limitManagementService.deleteLimit(userId, limitId));

        verify(userManagerService).getUserByIdOrThrow(userId);
        verify(limitRepository).findByIdAndUserAssignedId(user.getId(), limitId);
        verifyNoInteractions(limitActivityService);
        verify(limitRepository, never()).delete(any());
    }
}