package com.finovara.corebackend.limit.service;

import com.finovara.activityservice.contracts.event.limit.LimitActivityEvent;
import com.finovara.activityservice.contracts.model.activity.LimitActivityType;
import com.finovara.corebackend.limit.dto.LimitDto;
import com.finovara.corebackend.limit.dto.LimitStatsDto;
import com.finovara.corebackend.limit.exception.conflict.LimitAlreadyExistsException;
import com.finovara.corebackend.limit.exception.notfound.ActiveLimitNotFoundException;
import com.finovara.corebackend.limit.model.Limit;
import com.finovara.corebackend.limit.repository.LimitRepository;
import com.finovara.corebackend.user.exception.notfound.UserNotFoundException;
import com.finovara.corebackend.user.model.User;
import com.finovara.activityservice.contracts.model.PeriodType;
import com.finovara.corebackend.util.user.service.UserManagerService;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
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
    private KafkaTemplate<String, Object> kafkaTemplate;
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
            ArgumentCaptor<LimitActivityEvent> eventCaptor = ArgumentCaptor.forClass(LimitActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.limit"), eventCaptor.capture());
            assertEquals(LimitActivityType.ADDED_LIMIT, eventCaptor.getValue().type());
        }

        @Test
        void shouldThrowWhenLimitAlreadyExists() {
            LimitDto dto = new LimitDto(userId, null, PeriodType.DAILY, null, new BigDecimal("100"), true);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(limitRepository.findByUserAssignedIdAndType(userId, dto.periodType())).thenReturn(List.of(new Limit()));

            assertThrows(LimitAlreadyExistsException.class, () -> limitManagementService.createLimit(dto, userId));

            verifyNoInteractions(kafkaTemplate);
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
            ArgumentCaptor<LimitActivityEvent> eventCaptor = ArgumentCaptor.forClass(LimitActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.limit"), eventCaptor.capture());
            assertEquals(LimitActivityType.EDITED_LIMIT, eventCaptor.getValue().type());
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
            ArgumentCaptor<LimitActivityEvent> eventCaptor = ArgumentCaptor.forClass(LimitActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.limit"), eventCaptor.capture());
            assertEquals(LimitActivityType.DELETED_LIMIT, eventCaptor.getValue().type());
        }

        @Test
        void shouldThrowExceptionWhenLimitNotFound() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(limitRepository.findByIdAndUserAssignedId(userId, 10L)).thenReturn(Optional.empty());

            assertThrows(ActiveLimitNotFoundException.class, () -> limitManagementService.deleteLimit(userId, 10L));

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new UserNotFoundException("User not found"));

            assertThrows(UserNotFoundException.class, () -> limitManagementService.deleteLimit(userId, 10L));
        }

    }
}