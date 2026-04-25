package com.finovara.finovarabackend.accountactivity.piggybank.service;

import com.finovara.finovarabackend.accountactivity.piggybank.dto.PiggyBankActivityDto;
import com.finovara.finovarabackend.accountactivity.piggybank.mapper.PiggyBankActivityMapper;
import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivity;
import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.repository.PiggyBankActivityRepository;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.SortType;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PiggyBankActivityServiceTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private PiggyBankActivityRepository piggyBankActivityRepository;
    @Mock
    private PiggyBankActivityMapper piggyBankActivityMapper;

    @InjectMocks
    private PiggyBankActivityService piggyBankActivityService;

    private static final Long USER_ID = 1L;

    private User user;
    private PiggyBank piggyBank;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);

        piggyBank = new PiggyBank();
        piggyBank.setName("Vacation Fund");
        piggyBank.setGoalType(PiggyBankGoalType.ELECTRONICS);
        piggyBank.setGoalAmount(new BigDecimal("2000"));

        ReflectionTestUtils.setField(piggyBankActivityService, "pageSize", 10);
    }

    @Nested
    class CreateSimplePiggyBankActivity {

        @Test
        void shouldCreateSimplePiggyBankActivitySuccessfully() {
            LocalDateTime now = LocalDateTime.now();

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            piggyBankActivityService.createSimplePiggyBankActivity(USER_ID, piggyBank, PiggyBankActivityType.ADDED_PIGGY_BANK);

            ArgumentCaptor<PiggyBankActivity> captor = ArgumentCaptor.forClass(PiggyBankActivity.class);

            verify(piggyBankActivityRepository).save(captor.capture());

            PiggyBankActivity activity = captor.getValue();

            assertEquals(user, activity.getUserAssigned());
            assertEquals("Vacation Fund", activity.getPiggyBankName());
            assertEquals(PiggyBankActivityType.ADDED_PIGGY_BANK, activity.getActivityType());
            assertEquals(PiggyBankGoalType.ELECTRONICS, activity.getGoalType());
            assertEquals(0, activity.getGoalAmount().compareTo(new BigDecimal("2000")));
            assertTrue(!activity.getCreatedAt().isBefore(now));
        }

        @Test
        void shouldThrowExceptionWhenUserDoesNotExist() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new UserNotFoundException("User not found"));

            assertThrows(UserNotFoundException.class, () -> piggyBankActivityService.createSimplePiggyBankActivity(USER_ID, piggyBank,
                    PiggyBankActivityType.ADDED_PIGGY_BANK));

            verify(piggyBankActivityRepository, never()).save(any());
        }
    }

    @Nested
    class CreatePaymentPiggyBankActivity {

        @Test
        void shouldCreatePaymentPiggyBankActivitySuccessfully() {
            LocalDateTime now = LocalDateTime.now();
            BigDecimal paidAmount = new BigDecimal("500");

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            piggyBankActivityService.createPaymentPiggyBankActivity(USER_ID, piggyBank, PiggyBankActivityType.ADDED_PIGGY_BANK, paidAmount);

            ArgumentCaptor<PiggyBankActivity> captor = ArgumentCaptor.forClass(PiggyBankActivity.class);

            verify(piggyBankActivityRepository).save(captor.capture());

            PiggyBankActivity activity = captor.getValue();

            assertEquals(user, activity.getUserAssigned());
            assertEquals("Vacation Fund", activity.getPiggyBankName());
            assertEquals(PiggyBankActivityType.ADDED_PIGGY_BANK, activity.getActivityType());
            assertEquals(PiggyBankGoalType.ELECTRONICS, activity.getGoalType());
            assertEquals(0, activity.getGoalAmount().compareTo(new BigDecimal("2000")));
            assertEquals(0, activity.getAmountPaid().compareTo(paidAmount));
            assertTrue(!activity.getCreatedAt().isBefore(now));
        }

        @Test
        void shouldThrowExceptionWhenUserDoesNotExist() {
            BigDecimal paidAmount = new BigDecimal("500");

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new UserNotFoundException("User not found"));

            assertThrows(UserNotFoundException.class, () -> piggyBankActivityService.createPaymentPiggyBankActivity(USER_ID, piggyBank,
                    PiggyBankActivityType.DELETED_PIGGY_BANK, paidAmount));

            verify(piggyBankActivityRepository, never()).save(any());
        }
    }

    @Nested
    class GetPiggyBankActivities {

        @Test
        void shouldReturnActivitiesSortedByNewest() {
            PiggyBankActivity activity = new PiggyBankActivity();

            PiggyBankActivityDto dto = new PiggyBankActivityDto("My piggybank", null,
                    PiggyBankActivityType.ADDED_PIGGY_BANK, PiggyBankGoalType.GIFTS, null, new BigDecimal("100"),
                    null, null, null, LocalDateTime.now());

            when(piggyBankActivityRepository.findByUserAssignedId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of(activity));

            when(piggyBankActivityMapper.mapToPiggyBankActivity(activity)).thenReturn(dto);

            List<PiggyBankActivityDto> result = piggyBankActivityService.getPiggyBankActivities(USER_ID, SortType.NEWEST);

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());

            verify(piggyBankActivityRepository).findByUserAssignedId(eq(USER_ID), any(Pageable.class));
            verify(piggyBankActivityMapper).mapToPiggyBankActivity(activity);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoActivities() {
            when(piggyBankActivityRepository.findByUserAssignedId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

            List<PiggyBankActivityDto> result = piggyBankActivityService.getPiggyBankActivities(USER_ID, SortType.OLDEST);

            assertTrue(result.isEmpty());

            verify(piggyBankActivityRepository).findByUserAssignedId(eq(USER_ID), any(Pageable.class));
            verifyNoInteractions(piggyBankActivityMapper);
        }
    }
}