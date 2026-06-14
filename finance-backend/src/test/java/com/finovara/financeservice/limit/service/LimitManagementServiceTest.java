package com.finovara.financeservice.limit.service;

import com.finovara.contracts.event.activity.limit.LimitActivityEvent;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.model.activity.LimitActivityType;
import com.finovara.authservice.limit.dto.LimitDto;
import com.finovara.authservice.limit.dto.LimitStatsDto;
import com.finovara.authservice.limit.model.Limit;
import com.finovara.authservice.limit.repository.LimitRepository;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.PeriodType;
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
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private LimitCalculateService limitCalculateService;

    @InjectMocks
    private LimitManagementService limitManagementService;

    private Long userId;
    private Long limitId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        limitId = 1L;
    }

    @Nested
    class AddLimitTests {
        @Test
        void shouldCreateLimitSuccessfully() {
            LimitDto dto = new LimitDto(userId, null, PeriodType.DAILY, null, new BigDecimal("100"), true);
            when(limitRepository.findByUserIdAndType(userId, dto.periodType())).thenReturn(List.of());

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
            when(limitRepository.findByUserIdAndType(userId, dto.periodType())).thenReturn(List.of(new Limit()));

            assertThrows(EntityAlreadyExistsException.class, () -> limitManagementService.createLimit(dto, userId));

            verifyNoInteractions(kafkaTemplate);
        }

    }

    @Nested
    class EditLimitTests {
        @Test
        void shouldEditLimitSuccessfully() {
            LimitDto dto = new LimitDto(userId, null, null, null, new BigDecimal("200"), true);
            Limit limit = new Limit();
            limit.setId(limitId);
            limit.setUserId(userId);
            limit.setAmount(new BigDecimal("100"));
            when(limitRepository.findByIdAndUserId(userId, limitId)).thenReturn(Optional.of(limit));
            when(limitRepository.save(any(Limit.class))).thenReturn(limit);

            Long result = limitManagementService.editLimit(dto, limitId, userId);

            assertEquals(limitId, result);
            assertEquals(dto.amount(), limit.getAmount());
            assertEquals(dto.periodType(), limit.getPeriodType());
            verify(limitRepository).findByIdAndUserId(userId, limitId);
            ArgumentCaptor<LimitActivityEvent> eventCaptor = ArgumentCaptor.forClass(LimitActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.limit"), eventCaptor.capture());
            assertEquals(LimitActivityType.EDITED_LIMIT, eventCaptor.getValue().type());
            verify(limitRepository).save(limit);
        }

        @Test
        void shouldThrowExceptionWhenLimitMissing() {
            when(limitRepository.findByIdAndUserId(userId, 1L)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () ->
                    limitManagementService.editLimit(new LimitDto(userId, null, null, null, BigDecimal.TEN, true), 1L, userId));
        }

    }

    @Nested
    class GetLimitTests {

        @Test
        void shouldReturnStats() {
            Limit limit = new Limit();
            limit.setId(10L);
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of(limit));

            LimitStatsDto dto = new LimitStatsDto(10L, null, BigDecimal.valueOf(100), BigDecimal.valueOf(30),
                    BigDecimal.valueOf(70), BigDecimal.valueOf(30), null, LocalDate.now());

            when(limitCalculateService.calculateLimitStats(userId, 10L, LocalDate.now())).thenReturn(dto);

            List<LimitStatsDto> result = limitManagementService.getLimitStats(userId);

            assertThat(result, contains(dto));
        }

        @Test
        void shouldReturnEmptyList() {
            when(limitRepository.findAllByUserId(userId)).thenReturn(List.of());

            List<LimitStatsDto> result = limitManagementService.getLimitStats(userId);

            assertThat(result, is(empty()));
        }

    }

    @Nested
    class DeleteLimitTests {

        @Test
        void shouldDeleteSuccessfully() {
            Long limitId = 10L;

            Limit limit = new Limit();
            limit.setId(limitId);
            when(limitRepository.findByIdAndUserId(userId, limitId)).thenReturn(Optional.of(limit));

            limitManagementService.deleteLimit(userId, limitId);

            verify(limitRepository).delete(limit);
            ArgumentCaptor<LimitActivityEvent> eventCaptor = ArgumentCaptor.forClass(LimitActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.limit"), eventCaptor.capture());
            assertEquals(LimitActivityType.DELETED_LIMIT, eventCaptor.getValue().type());
        }

        @Test
        void shouldThrowExceptionWhenLimitNotFound() {
            when(limitRepository.findByIdAndUserId(userId, 10L)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> limitManagementService.deleteLimit(userId, 10L));

            verifyNoInteractions(kafkaTemplate);
        }

    }
}