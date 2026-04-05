package com.finovara.finovarabackend.limit.management.edit;

import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivityType;
import com.finovara.finovarabackend.accountactivity.limit.service.LimitActivityService;
import com.finovara.finovarabackend.limit.dto.LimitDTO;
import com.finovara.finovarabackend.limit.exception.notfound.ActiveLimitNotFoundException;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.limit.service.LimitManagementService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EditLimitTest {

    @Mock
    private LimitRepository limitRepository;
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private LimitActivityService limitActivityService;

    @InjectMocks
    private LimitManagementService limitManagementService;

    private String email;
    private Long limitId;
    private Long userId;

    @BeforeEach
    void setUp() {
        email = "test@test.com";
        limitId = 1L;
        userId = 1L;
    }

    @Test
    void shouldEditLimitSuccessfully() {
        LimitDTO dto = new LimitDTO(userId, null, null, null, new BigDecimal("200"), true);

        User user = new User();
        user.setId(userId);

        Limit limit = new Limit();
        limit.setId(limitId);
        limit.setUserAssigned(user);
        limit.setAmount(new BigDecimal("100"));

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(limitRepository.findByIdAndUserAssignedId(userId, limitId)).thenReturn(Optional.of(limit));
        when(limitRepository.save(any(Limit.class))).thenReturn(limit);

        Long result = limitManagementService.editLimit(dto, limitId, email);

        assertEquals(limitId, result);
        assertEquals(dto.amount(), limit.getAmount());
        assertEquals(dto.periodType(), limit.getPeriodType());

        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(limitRepository).findByIdAndUserAssignedId(userId, limitId);
        verify(limitActivityService).updateLimitActivity(email, LimitActivityType.EDITED_LIMIT, limit, new BigDecimal("100"));
        verify(limitRepository).save(limit);
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        LimitDTO dto = new LimitDTO(userId, null, null, null, new BigDecimal("200"), true);
        when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> limitManagementService.editLimit(dto, limitId, email));

        verify(userManagerService).getUserByEmailOrThrow(email);
        verifyNoInteractions(limitRepository, limitActivityService);
        verify(limitRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenLimitDoesNotExist() {
        LimitDTO dto = new LimitDTO(userId, null, null, null, new BigDecimal("200"), true);
        User user = new User();
        user.setId(userId);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(limitRepository.findByIdAndUserAssignedId(userId, limitId)).thenReturn(Optional.empty());

        assertThrows(ActiveLimitNotFoundException.class, () -> limitManagementService.editLimit(dto, limitId, email));

        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(limitRepository).findByIdAndUserAssignedId(userId, limitId);
        verifyNoInteractions(limitActivityService);
        verify(limitRepository, never()).save(any());
    }
}