package com.finovara.finovarabackend.limit.service;

import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivityType;
import com.finovara.finovarabackend.accountactivity.limit.service.LimitActivityService;
import com.finovara.finovarabackend.limit.dto.LimitDto;
import com.finovara.finovarabackend.limit.dto.LimitStatsDto;
import com.finovara.finovarabackend.limit.exception.conflict.LimitAlreadyExistsException;
import com.finovara.finovarabackend.limit.exception.notfound.ActiveLimitNotFoundException;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class LimitManagementServiceTest {

    @Mock
    private LimitRepository limitRepository;
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private LimitActivityService limitActivityService;
    @Mock
    private LimitCalculateService limitCalculateService;

    @InjectMocks
    private LimitManagementService limitManagementService;

    private Long userId;
    private User user;
    private Long limitId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        limitId = 1L;
        user = new User();
        user.setId(userId);
    }

    @Nested
    class AddLimitTests {
        @Test
        void shouldCreateLimitSuccessfully() {
            LimitDto dto = new LimitDto(userId, null, PeriodType.DAILY, null, new BigDecimal("100"), true);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(limitRepository.findByUserAssignedIdAndType(userId, dto.periodType())).thenReturn(List.of());

            Limit saved = new Limit();
            saved.setId(10L);

            when(limitRepository.save(any())).thenReturn(saved);

            Long result = limitManagementService.createLimit(dto, userId);

            assertEquals(10L, result);
            verify(limitActivityService).createLimitActivity(eq(userId), eq(LimitActivityType.ADDED_LIMIT), any());
        }

        @Test
        void shouldThrowWhenLimitAlreadyExists() {
            LimitDto dto = new LimitDto(userId, null, PeriodType.DAILY, null, new BigDecimal("100"), true);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(limitRepository.findByUserAssignedIdAndType(userId, dto.periodType())).thenReturn(List.of(new Limit()));

            assertThrows(LimitAlreadyExistsException.class, () -> limitManagementService.createLimit(dto, userId));

            verifyNoInteractions(limitActivityService);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            LimitDto dto = new LimitDto(null, null, PeriodType.DAILY, null, new BigDecimal("100"), true);

            when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new UserNotFoundException("User not found"));

            assertThrows(UserNotFoundException.class, () -> limitManagementService.createLimit(dto, userId));
        }
    }

    @Nested
    class EditLimitTests {
        @Test
        void shouldEditLimitSuccessfully() {
            LimitDto dto = new LimitDto(userId, null, null, null, new BigDecimal("200"), true);
            Limit limit = new Limit();
            limit.setId(limitId);
            limit.setUserAssigned(user);
            limit.setAmount(new BigDecimal("100"));

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(limitRepository.findByIdAndUserAssignedId(userId, limitId)).thenReturn(Optional.of(limit));
            when(limitRepository.save(any(Limit.class))).thenReturn(limit);

            Long result = limitManagementService.editLimit(dto, limitId, userId);

            assertEquals(limitId, result);
            assertEquals(dto.amount(), limit.getAmount());
            assertEquals(dto.periodType(), limit.getPeriodType());

            verify(userManagerService).getUserByIdOrThrow(userId);
            verify(limitRepository).findByIdAndUserAssignedId(userId, limitId);
            verify(limitActivityService).updateLimitActivity(userId, LimitActivityType.EDITED_LIMIT, limit, new BigDecimal("100"));
            verify(limitRepository).save(limit);
        }

        @Test
        void shouldThrowExceptionWhenLimitMissing() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(limitRepository.findByIdAndUserAssignedId(userId, 1L)).thenReturn(Optional.empty());

            assertThrows(ActiveLimitNotFoundException.class, () ->
                    limitManagementService.editLimit(new LimitDto(userId, null, null, null, BigDecimal.TEN, true), 1L, userId));
        }

    }

    @Nested
    class GetLimitTests {

        @Test
        void shouldReturnStats() {
            Limit limit = new Limit();
            limit.setId(10L);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(limitRepository.findAllByUserAssignedId(userId)).thenReturn(List.of(limit));

            LimitStatsDto dto = new LimitStatsDto(10L, null, BigDecimal.valueOf(100), BigDecimal.valueOf(30),
                    BigDecimal.valueOf(70), BigDecimal.valueOf(30), null, LocalDate.now());

            when(limitCalculateService.calculateLimitStats(userId, 10L, LocalDate.now())).thenReturn(dto);

            List<LimitStatsDto> result = limitManagementService.getLimitStats(userId);

            assertThat(result, contains(dto));
        }

        @Test
        void shouldReturnEmptyList() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(limitRepository.findAllByUserAssignedId(userId)).thenReturn(List.of());

            List<LimitStatsDto> result = limitManagementService.getLimitStats(userId);

            assertThat(result, is(empty()));
        }

        @Test
        void shouldThrowUserNotFound() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new UserNotFoundException("User not found"));

            assertThrows(UserNotFoundException.class, () -> limitManagementService.getLimitStats(userId));
        }
    }

    @Nested
    class DeleteLimitTests {

        @Test
        void shouldDeleteSuccessfully() {
            Long limitId = 10L;

            Limit limit = new Limit();
            limit.setId(limitId);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(limitRepository.findByIdAndUserAssignedId(userId, limitId)).thenReturn(Optional.of(limit));

            limitManagementService.deleteLimit(userId, limitId);

            verify(limitRepository).delete(limit);
            verify(limitActivityService).createLimitActivity(userId, LimitActivityType.DELETED_LIMIT, limit);
        }

        @Test
        void shouldThrowExceptionWhenLimitNotFound() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(limitRepository.findByIdAndUserAssignedId(userId, 10L)).thenReturn(Optional.empty());

            assertThrows(ActiveLimitNotFoundException.class, () -> limitManagementService.deleteLimit(userId, 10L));

            verifyNoInteractions(limitActivityService);
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new UserNotFoundException("User not found"));

            assertThrows(UserNotFoundException.class, () -> limitManagementService.deleteLimit(userId, 10L));
        }

    }
}