package com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.contracts.sharedaccount.event.notification.GoalAchievedNotificationEvent;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.dto.GoalAchievedNotificationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalAchievedNotificationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OWNER_ID = 10L;
    private static final Long MEMBER_ID = 20L;
    private static final Long PIGGY_BANK_ID = 30L;
    private static final String SETTINGS_AGGREGATE = "SharedAccountSettings";
    private static final String ACTIVITY_TOPIC = "shared-account.activity";
    private static final String PIGGY_BANK_AGGREGATE = "PiggyBank";
    private static final String NOTIFICATION_TOPIC = "notification.shared-account.piggy-bank-goal-achieved";
    private static final BigDecimal GOAL_AMOUNT = new BigDecimal("100.00");

    @Mock
    private SharedAccountSettingsRepository sharedAccountSettingsRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private SharedAccountSettings sharedAccountSettings;

    @Mock
    private SharedAccountParticipantsResponse participantsResponse;

    @Mock
    private SharedPiggyBank sharedPiggyBank;

    @Captor
    private ArgumentCaptor<SharedAccountActivityLogEvent> activityEventCaptor;

    @InjectMocks
    private GoalAchievedNotificationService service;

    @BeforeEach
    void setUp() {
        when(sharedAccountSettingsRepository.findByUserId(USER_ID)).thenReturn(sharedAccountSettings);
    }

    @Nested
    class SaveGoalAchievedNotification {

        private void stubParticipants() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
            when(participantsResponse.ownerId()).thenReturn(OWNER_ID);
            when(participantsResponse.memberId()).thenReturn(MEMBER_ID);
        }

        @ParameterizedTest
        @CsvSource({"true,false", "false,true"})
        void shouldUpdateSettingWhenValueChanges(boolean current, boolean requested) {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(current);
            stubParticipants();

            service.saveGoalAchievedNotification(USER_ID, new GoalAchievedNotificationDto(requested));

            verify(sharedAccountSettings).setPiggyBankGoalAchievedNotificationEnabled(requested);
        }

        @ParameterizedTest
        @CsvSource({"true,false", "false,true"})
        void shouldSaveActivityEventWhenValueChanges(boolean current, boolean requested) {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(current);
            stubParticipants();

            service.saveGoalAchievedNotification(USER_ID, new GoalAchievedNotificationDto(requested));

            verify(outboxService).save(eq(SETTINGS_AGGREGATE), eq(USER_ID.toString()), eq(ACTIVITY_TOPIC),
                    activityEventCaptor.capture());
            SharedAccountActivityLogEvent event = activityEventCaptor.getValue();
            assertEquals(OWNER_ID, event.ownerId());
            assertEquals(MEMBER_ID, event.memberId());
            assertEquals(USER_ID, event.userId());
            assertNull(event.targetId());
            assertEquals(SharedAccountActivityLogType.SETTING_CHANGED, event.type());
            assertNotNull(event.createdAt());
        }

        @Test
        void shouldUpdateSettingBeforeSavingActivityEventWhenValueChanges() {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(false);
            stubParticipants();

            service.saveGoalAchievedNotification(USER_ID, new GoalAchievedNotificationDto(true));

            InOrder inOrder = inOrder(sharedAccountSettings, outboxService);
            inOrder.verify(sharedAccountSettings).setPiggyBankGoalAchievedNotificationEnabled(true);
            inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void shouldDoNothingWhenValueDoesNotChange(boolean value) {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(value);

            service.saveGoalAchievedNotification(USER_ID, new GoalAchievedNotificationDto(value));

            verify(sharedAccountSettings, never()).setPiggyBankGoalAchievedNotificationEnabled(value);
            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenSettingsDtoIsNull() {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(true);

            assertThrows(NullPointerException.class, () -> service.saveGoalAchievedNotification(USER_ID, null));

            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenSettingsAreNotFound() {
            when(sharedAccountSettingsRepository.findByUserId(USER_ID)).thenReturn(null);

            assertThrows(NullPointerException.class,
                    () -> service.saveGoalAchievedNotification(USER_ID, new GoalAchievedNotificationDto(true)));

            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(sharedAccountSettingsRepository.findByUserId(USER_ID)).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class,
                    () -> service.saveGoalAchievedNotification(USER_ID, new GoalAchievedNotificationDto(true)));

            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsServiceFails() {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(false);
            when(sharedAccountParticipantsService.getParticipants(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("participants not found"));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.saveGoalAchievedNotification(USER_ID, new GoalAchievedNotificationDto(true)));

            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsResponseIsNull() {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(false);

            assertThrows(NullPointerException.class,
                    () -> service.saveGoalAchievedNotification(USER_ID, new GoalAchievedNotificationDto(true)));

            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenOutboxServiceFails() {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(false);
            stubParticipants();
            doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                    .save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class,
                    () -> service.saveGoalAchievedNotification(USER_ID, new GoalAchievedNotificationDto(true)));
        }
    }

    @Nested
    class GetGoalAchievedNotification {

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void shouldReturnDtoWithSettingValueWhenSettingsExist(boolean enabled) {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(enabled);

            GoalAchievedNotificationDto result = service.getGoalAchievedNotification(USER_ID);

            assertEquals(enabled, result.piggyBankGoalAchievedNotificationEnabled());
        }

        @Test
        void shouldNotInteractWithOutboxWhenGettingNotification() {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(true);

            service.getGoalAchievedNotification(USER_ID);

            verifyNoInteractions(outboxService, sharedAccountParticipantsService);
        }

        @Test
        void shouldThrowExceptionWhenSettingsAreNotFound() {
            when(sharedAccountSettingsRepository.findByUserId(USER_ID)).thenReturn(null);

            assertThrows(NullPointerException.class, () -> service.getGoalAchievedNotification(USER_ID));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(sharedAccountSettingsRepository.findByUserId(USER_ID)).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.getGoalAchievedNotification(USER_ID));
        }
    }

    @Nested
    class HandleGoalAchieved {

        private void stubEnabledAndNotNotified() {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(true);
            when(sharedPiggyBank.isGoalAchievedNotified()).thenReturn(false);
        }

        private void stubAmounts(String amount) {
            when(sharedPiggyBank.getAmount()).thenReturn(new BigDecimal(amount));
            when(sharedPiggyBank.getGoalAmount()).thenReturn(GOAL_AMOUNT);
        }

        private void stubEventData() {
            when(sharedPiggyBank.getOwnerId()).thenReturn(OWNER_ID);
            when(sharedPiggyBank.getMemberId()).thenReturn(MEMBER_ID);
            when(sharedPiggyBank.getId()).thenReturn(PIGGY_BANK_ID);
        }

        @ParameterizedTest
        @ValueSource(strings = {"100.00", "100.01", "250.00"})
        void shouldMarkPiggyBankAsNotifiedWhenGoalIsCompleted(String amount) {
            stubEnabledAndNotNotified();
            stubAmounts(amount);
            stubEventData();

            service.handleGoalAchieved(USER_ID, sharedPiggyBank);

            verify(sharedPiggyBank).setGoalAchievedNotified(true);
        }

        @Test
        void shouldSaveNotificationEventWhenGoalIsCompleted() {
            stubEnabledAndNotNotified();
            stubAmounts("100.00");
            stubEventData();

            service.handleGoalAchieved(USER_ID, sharedPiggyBank);

            verify(outboxService).save(eq(PIGGY_BANK_AGGREGATE), eq(USER_ID.toString()), eq(NOTIFICATION_TOPIC),
                    any(GoalAchievedNotificationEvent.class));
        }

        @Test
        void shouldMarkAsNotifiedBeforeSavingNotificationEventWhenGoalIsCompleted() {
            stubEnabledAndNotNotified();
            stubAmounts("100.00");
            stubEventData();

            service.handleGoalAchieved(USER_ID, sharedPiggyBank);

            InOrder inOrder = inOrder(sharedPiggyBank, outboxService);
            inOrder.verify(sharedPiggyBank).setGoalAchievedNotified(true);
            inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
        }

        @Test
        void shouldNotNotifyWhenGoalIsNotCompleted() {
            stubEnabledAndNotNotified();
            stubAmounts("99.99");

            service.handleGoalAchieved(USER_ID, sharedPiggyBank);

            verify(sharedPiggyBank, never()).setGoalAchievedNotified(true);
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldNotNotifyWhenNotificationIsDisabled() {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(false);

            service.handleGoalAchieved(USER_ID, sharedPiggyBank);

            verifyNoInteractions(sharedPiggyBank, outboxService);
        }

        @Test
        void shouldNotThrowExceptionWhenNotificationIsDisabledAndPiggyBankIsNull() {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(false);

            service.handleGoalAchieved(USER_ID, null);

            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldNotNotifyWhenGoalWasAlreadyNotified() {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(true);
            when(sharedPiggyBank.isGoalAchievedNotified()).thenReturn(true);

            service.handleGoalAchieved(USER_ID, sharedPiggyBank);

            verify(sharedPiggyBank, never()).setGoalAchievedNotified(true);
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankIsNullAndNotificationIsEnabled() {
            when(sharedAccountSettings.isPiggyBankGoalAchievedNotificationEnabled()).thenReturn(true);

            assertThrows(NullPointerException.class, () -> service.handleGoalAchieved(USER_ID, null));

            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenSettingsAreNotFound() {
            when(sharedAccountSettingsRepository.findByUserId(USER_ID)).thenReturn(null);

            assertThrows(NullPointerException.class, () -> service.handleGoalAchieved(USER_ID, sharedPiggyBank));

            verifyNoInteractions(sharedPiggyBank, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(sharedAccountSettingsRepository.findByUserId(USER_ID)).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.handleGoalAchieved(USER_ID, sharedPiggyBank));

            verifyNoInteractions(sharedPiggyBank, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenOutboxServiceFails() {
            stubEnabledAndNotNotified();
            stubAmounts("100.00");
            stubEventData();
            doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                    .save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class, () -> service.handleGoalAchieved(USER_ID, sharedPiggyBank));

            assertTrue(true);
        }
    }
}