package com.finovara.activitylogservice.accountactivity.piggybank.service;

import com.finovara.activitylogservice.activitylog.accountactivity.piggybank.dto.PiggyBankActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.piggybank.mapper.PiggyBankActivityMapper;
import com.finovara.activitylogservice.activitylog.accountactivity.piggybank.model.PiggyBankActivity;
import com.finovara.activitylogservice.activitylog.accountactivity.piggybank.repository.PiggyBankActivityRepository;
import com.finovara.activitylogservice.activitylog.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.model.SortType;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PiggyBankActivityServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 5, 25, 13, 0);

    @Mock
    private PiggyBankActivityRepository piggyBankActivityRepository;

    @Mock
    private PiggyBankActivityMapper piggyBankActivityMapper;

    @InjectMocks
    private PiggyBankActivityService piggyBankActivityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(piggyBankActivityService, "pageSize", 10);
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldCallRepositoryDeleteByUserIdWhenUserIdIsValid() {
            piggyBankActivityService.deleteByUserId(USER_ID);

            verify(piggyBankActivityRepository).deleteByUserId(USER_ID);
        }
    }

    @Nested
    class HandleEvent {

        @Test
        void shouldBuildEntityAndSaveViaRepository() {
            PiggyBankActivityEvent event = new PiggyBankActivityEvent(
                    USER_ID,
                    PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY,
                    "Vacation Fund",
                    PiggyBankGoalType.VACATION,
                    new BigDecimal("2000.00"),
                    new BigDecimal("250.00"),
                    OCCURRED_AT
            );

            piggyBankActivityService.handleEvent(event);

            ArgumentCaptor<PiggyBankActivity> captor = ArgumentCaptor.forClass(PiggyBankActivity.class);
            verify(piggyBankActivityRepository).save(captor.capture());

            PiggyBankActivity activity = captor.getValue();
            assertThat(activity.getUserId()).isEqualTo(USER_ID);
            assertThat(activity.getActivityType()).isEqualTo(event.type());
            assertThat(activity.getPiggyBankName()).isEqualTo(event.name());
            assertThat(activity.getGoalType()).isEqualTo(event.goalType());
            assertThat(activity.getGoalAmount()).isEqualByComparingTo(event.goalAmount());
            assertThat(activity.getAmountPaid()).isEqualByComparingTo(event.amountPaid());
            assertThat(activity.getCreatedAt()).isEqualTo(OCCURRED_AT);
        }
    }

    @Nested
    class GetPiggyBankActivities {

        @Test
        void shouldReturnMappedActivities() {
            PiggyBankActivity activity = PiggyBankActivity.builder().userId(USER_ID).build();
            PiggyBankActivityDto dto = new PiggyBankActivityDto(
                    "Piggy",
                    null,
                    PiggyBankActivityType.ADDED_PIGGY_BANK,
                    PiggyBankGoalType.GIFTS,
                    null,
                    new BigDecimal("100.00"),
                    null,
                    null,
                    null,
                    OCCURRED_AT
            );

            when(piggyBankActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of(activity));
            when(piggyBankActivityMapper.mapToPiggyBankActivity(activity)).thenReturn(dto);

            List<PiggyBankActivityDto> result = piggyBankActivityService.getPiggyBankActivities(USER_ID, SortType.NEWEST);

            assertThat(result).containsExactly(dto);
            verify(piggyBankActivityRepository).findByUserId(eq(USER_ID), any(Pageable.class));
            verify(piggyBankActivityMapper).mapToPiggyBankActivity(activity);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoActivities() {
            when(piggyBankActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

            List<PiggyBankActivityDto> result = piggyBankActivityService.getPiggyBankActivities(USER_ID, SortType.OLDEST);

            assertThat(result).isEmpty();
            verifyNoInteractions(piggyBankActivityMapper);
        }
    }
}
