package com.finovara.finovarabackend.limit.management.add;

import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivityType;
import com.finovara.finovarabackend.accountactivity.limit.service.LimitActivityService;
import com.finovara.finovarabackend.limit.dto.LimitDto;
import com.finovara.finovarabackend.limit.exception.conflict.LimitAlreadyExistsException;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.util.model.PeriodType;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddLimitTest {
    @Mock
    private LimitRepository limitRepository;
    @Mock
    private UserManagerService userManagerService;

    @Mock
    private LimitActivityService limitActivityService;

    @InjectMocks
    private LimitManagementService limitManagementService;

    @Test
    void shouldCreateLimitSuccessfully() {

        Long userId = 1L;

        LimitDto dto = new LimitDto(userId, null, PeriodType.DAILY, null, new BigDecimal("100"), true);

        User user = new User();
        user.setId(userId);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(limitRepository.findByUserAssignedIdAndType(userId, dto.periodType()))
                .thenReturn(Collections.emptyList());

        Limit savedLimit = new Limit();
        savedLimit.setId(10L);

        when(limitRepository.save(any(Limit.class))).thenReturn(savedLimit);

        Long result = limitManagementService.createLimit(dto, userId);

        assertEquals(10L, result);

        verify(userManagerService).getUserByIdOrThrow(userId);
        verify(limitRepository).findByUserAssignedIdAndType(userId, dto.periodType());
        verify(limitActivityService).createLimitActivity(eq(userId), eq(LimitActivityType.ADDED_LIMIT), any(Limit.class));
        verify(limitRepository).save(any(Limit.class));
    }

    @Test
    void shouldThrowExceptionWhenLimitAlreadyExists() {

        Long userId = 1L;

        LimitDto dto = new LimitDto(userId, null, PeriodType.DAILY, null, new BigDecimal("100"), true);

        User user = new User();
        user.setId(userId);

        Limit existingLimit = new Limit();

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(limitRepository.findByUserAssignedIdAndType(userId, dto.periodType())).thenReturn(List.of(existingLimit));
        assertThrows(LimitAlreadyExistsException.class, () -> limitManagementService.createLimit(dto, userId));
        verify(limitRepository).findByUserAssignedIdAndType(userId, dto.periodType());
        verify(limitRepository, never()).save(any());
        verifyNoInteractions(limitActivityService);
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        Long userId = 1L;
        LimitDto dto = new LimitDto(null, null, PeriodType.DAILY, null, new BigDecimal("100"), true);

        when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> limitManagementService.createLimit(dto, userId));
        verify(userManagerService).getUserByIdOrThrow(userId);
        verify(limitRepository, never()).save(any());
    }
}