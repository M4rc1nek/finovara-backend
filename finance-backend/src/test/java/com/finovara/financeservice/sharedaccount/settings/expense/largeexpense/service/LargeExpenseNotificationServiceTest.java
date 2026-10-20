package com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.contracts.sharedaccount.event.notification.LargeExpenseNotificationEvent;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.dto.LargeExpenseNotificationDto;
import com.finovara.financeservice.util.sharedaccount.settings.SharedSettingsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LargeExpenseNotificationServiceTest {

    private static final Long USER_ID = 3L;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long EXPENSE_ID = 20L;
    private static final String SETTINGS_AGGREGATE = "SharedAccountSettings";
    private static final String ACTIVITY_TOPIC = "shared-account.activity";
    private static final String EXPENSE_AGGREGATE = "SharedExpense";
    private static final String NOTIFICATION_TOPIC = "notification.shared-account.large-expense-detected";
    private static final BigDecimal THRESHOLD = new BigDecimal("100.00");
    private static final BigDecimal HIGHER_THRESHOLD = new BigDecimal("200.00");
    private static final BigDecimal LARGE_AMOUNT = new BigDecimal("150.00");

    @Mock
    private SharedSettingsManager sharedSettingsManager;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private SharedAccountParticipantsResponse participantsResponse;

    @Captor
    private ArgumentCaptor<SharedAccountActivityLogEvent> activityEventCaptor;

    @Captor
    private ArgumentCaptor<LargeExpenseNotificationEvent> notificationEventCaptor;

    @InjectMocks
    private LargeExpenseNotificationService largeExpenseNotificationService;

    private SharedAccountSettings settings;
    private SharedExpense expense;

    @BeforeEach
    void setUp() {
        settings = SharedAccountSettings.builder()
                .id(10L)
                .ownerId(OWNER_ID)
                .memberId(MEMBER_ID)
                .largeExpenseNotificationEnabled(true)
                .largeExpenseNotificationThreshold(THRESHOLD)
                .build();
        expense = SharedExpense.builder()
                .id(EXPENSE_ID)
                .ownerId(OWNER_ID)
                .memberId(MEMBER_ID)
                .amount(LARGE_AMOUNT)
                .build();
    }

    @Nested
    class SaveLargeExpenseNotification {

        @Test
        void shouldThrowExceptionWhenSettingsAreNotFound() {
            LargeExpenseNotificationDto dto = new LargeExpenseNotificationDto(false, HIGHER_THRESHOLD);

            doThrow(new RequestedEntityNotFoundException("settings not found")).when(sharedSettingsManager).getSharedAccountSettingsByUserId(USER_ID);

            assertThrows(RequestedEntityNotFoundException.class, () -> largeExpenseNotificationService.saveLargeExpenseNotification(USER_ID, dto));

            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Nested
        class WithExistingSettings {

            @BeforeEach
            void setUp() {
                when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenReturn(settings);
            }

            @Test
            void shouldNotSaveEventWhenValuesAreUnchanged() {
                LargeExpenseNotificationDto dto = new LargeExpenseNotificationDto(true, THRESHOLD);

                largeExpenseNotificationService.saveLargeExpenseNotification(USER_ID, dto);

                assertTrue(settings.isLargeExpenseNotificationEnabled());
                assertEquals(THRESHOLD, settings.getLargeExpenseNotificationThreshold());
                verifyNoInteractions(sharedAccountParticipantsService, outboxService);
            }

            @Test
            void shouldNotSaveEventWhenBothThresholdsAreNullAndFlagIsUnchanged() {
                settings.setLargeExpenseNotificationThreshold(null);
                LargeExpenseNotificationDto dto = new LargeExpenseNotificationDto(true, null);

                largeExpenseNotificationService.saveLargeExpenseNotification(USER_ID, dto);

                assertNull(settings.getLargeExpenseNotificationThreshold());
                verifyNoInteractions(sharedAccountParticipantsService, outboxService);
            }

            @Test
            void shouldNotSaveEventWhenNotificationIsDisabledAndValuesAreUnchanged() {
                settings.setLargeExpenseNotificationEnabled(false);
                LargeExpenseNotificationDto dto = new LargeExpenseNotificationDto(false, THRESHOLD);

                largeExpenseNotificationService.saveLargeExpenseNotification(USER_ID, dto);

                assertFalse(settings.isLargeExpenseNotificationEnabled());
                verifyNoInteractions(sharedAccountParticipantsService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenParticipantsServiceFails() {
                LargeExpenseNotificationDto dto = new LargeExpenseNotificationDto(false, THRESHOLD);

                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenThrow(new RequestedEntityNotFoundException("participants not found"));

                assertThrows(RequestedEntityNotFoundException.class, () -> largeExpenseNotificationService.saveLargeExpenseNotification(USER_ID, dto));

                verifyNoInteractions(outboxService);
            }

            @Nested
            class WhenSettingsChange {

                @BeforeEach
                void setUp() {
                    when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
                    when(participantsResponse.ownerId()).thenReturn(OWNER_ID);
                    when(participantsResponse.memberId()).thenReturn(MEMBER_ID);
                }

                @Test
                void shouldUpdateOnlyEnabledFlagWhenOnlyEnabledFlagChanges() {
                    LargeExpenseNotificationDto dto = new LargeExpenseNotificationDto(false, THRESHOLD);

                    largeExpenseNotificationService.saveLargeExpenseNotification(USER_ID, dto);

                    assertFalse(settings.isLargeExpenseNotificationEnabled());
                    assertEquals(THRESHOLD, settings.getLargeExpenseNotificationThreshold());
                    verify(outboxService).save(eq(SETTINGS_AGGREGATE), eq(USER_ID.toString()), eq(ACTIVITY_TOPIC), any(SharedAccountActivityLogEvent.class));
                }

                @Test
                void shouldUpdateOnlyThresholdWhenOnlyThresholdChanges() {
                    LargeExpenseNotificationDto dto = new LargeExpenseNotificationDto(true, HIGHER_THRESHOLD);

                    largeExpenseNotificationService.saveLargeExpenseNotification(USER_ID, dto);

                    assertTrue(settings.isLargeExpenseNotificationEnabled());
                    assertEquals(HIGHER_THRESHOLD, settings.getLargeExpenseNotificationThreshold());
                    verify(outboxService).save(eq(SETTINGS_AGGREGATE), eq(USER_ID.toString()), eq(ACTIVITY_TOPIC), any(SharedAccountActivityLogEvent.class));
                }

                @Test
                void shouldUpdateEnabledFlagAndThresholdWhenBothChange() {
                    LargeExpenseNotificationDto dto = new LargeExpenseNotificationDto(false, HIGHER_THRESHOLD);

                    largeExpenseNotificationService.saveLargeExpenseNotification(USER_ID, dto);

                    assertFalse(settings.isLargeExpenseNotificationEnabled());
                    assertEquals(HIGHER_THRESHOLD, settings.getLargeExpenseNotificationThreshold());
                    verify(outboxService).save(eq(SETTINGS_AGGREGATE), eq(USER_ID.toString()), eq(ACTIVITY_TOPIC), any(SharedAccountActivityLogEvent.class));
                }

                @Test
                void shouldUpdateThresholdWhenPreviousThresholdIsNull() {
                    settings.setLargeExpenseNotificationThreshold(null);
                    LargeExpenseNotificationDto dto = new LargeExpenseNotificationDto(true, THRESHOLD);

                    largeExpenseNotificationService.saveLargeExpenseNotification(USER_ID, dto);

                    assertEquals(THRESHOLD, settings.getLargeExpenseNotificationThreshold());
                    verify(outboxService).save(eq(SETTINGS_AGGREGATE), eq(USER_ID.toString()), eq(ACTIVITY_TOPIC), any(SharedAccountActivityLogEvent.class));
                }

                @Test
                void shouldClearThresholdWhenNewThresholdIsNull() {
                    LargeExpenseNotificationDto dto = new LargeExpenseNotificationDto(true, null);

                    largeExpenseNotificationService.saveLargeExpenseNotification(USER_ID, dto);

                    assertNull(settings.getLargeExpenseNotificationThreshold());
                    verify(outboxService).save(eq(SETTINGS_AGGREGATE), eq(USER_ID.toString()), eq(ACTIVITY_TOPIC), any(SharedAccountActivityLogEvent.class));
                }

                @Test
                void shouldSaveSettingChangedEventWithParticipantsDataWhenSettingsChange() {
                    LargeExpenseNotificationDto dto = new LargeExpenseNotificationDto(false, HIGHER_THRESHOLD);

                    largeExpenseNotificationService.saveLargeExpenseNotification(USER_ID, dto);

                    verify(outboxService).save(eq(SETTINGS_AGGREGATE), eq(USER_ID.toString()), eq(ACTIVITY_TOPIC), activityEventCaptor.capture());

                    SharedAccountActivityLogEvent event = activityEventCaptor.getValue();

                    assertEquals(OWNER_ID, event.ownerId());
                    assertEquals(MEMBER_ID, event.memberId());
                    assertEquals(USER_ID, event.userId());
                    assertNull(event.targetId());
                    assertEquals(SharedAccountActivityLogType.SETTING_CHANGED, event.type());
                    assertNotNull(event.createdAt());
                }

                @Test
                void shouldThrowExceptionWhenOutboxServiceFails() {
                    LargeExpenseNotificationDto dto = new LargeExpenseNotificationDto(false, HIGHER_THRESHOLD);

                    doThrow(new IllegalStateException("outbox failed")).when(outboxService).save(anyString(), anyString(), anyString(), any());

                    assertThrows(IllegalStateException.class, () -> largeExpenseNotificationService.saveLargeExpenseNotification(USER_ID, dto));
                }
            }
        }
    }

    @Nested
    class GetLargeExpenseNotification {

        @Test
        void shouldThrowExceptionWhenSettingsAreNotFound() {
            doThrow(new RequestedEntityNotFoundException("settings not found")).when(sharedSettingsManager).getSharedAccountSettingsByUserId(USER_ID);

            assertThrows(RequestedEntityNotFoundException.class, () -> largeExpenseNotificationService.getLargeExpenseNotification(USER_ID));

            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Nested
        class WithExistingSettings {

            @BeforeEach
            void setUp() {
                when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenReturn(settings);
            }

            @Test
            void shouldReturnNotificationSettingsWhenSettingsExist() {
                LargeExpenseNotificationDto result = largeExpenseNotificationService.getLargeExpenseNotification(USER_ID);

                assertEquals(Boolean.TRUE, result.largeExpenseNotificationEnabled());
                assertEquals(THRESHOLD, result.largeExpenseNotificationThreshold());
                verifyNoInteractions(sharedAccountParticipantsService, outboxService);
            }

            @Test
            void shouldReturnDisabledNotificationWhenNotificationIsDisabled() {
                settings.setLargeExpenseNotificationEnabled(false);

                LargeExpenseNotificationDto result = largeExpenseNotificationService.getLargeExpenseNotification(USER_ID);

                assertEquals(Boolean.FALSE, result.largeExpenseNotificationEnabled());
                assertEquals(THRESHOLD, result.largeExpenseNotificationThreshold());
            }

            @Test
            void shouldReturnNullThresholdWhenThresholdIsNotConfigured() {
                settings.setLargeExpenseNotificationThreshold(null);

                LargeExpenseNotificationDto result = largeExpenseNotificationService.getLargeExpenseNotification(USER_ID);

                assertEquals(Boolean.TRUE, result.largeExpenseNotificationEnabled());
                assertNull(result.largeExpenseNotificationThreshold());
            }
        }
    }

    @Nested
    class HandleLargeNotification {

        @Test
        void shouldThrowExceptionWhenSettingsAreNotFound() {
            doThrow(new RequestedEntityNotFoundException("settings not found")).when(sharedSettingsManager).getSharedAccountSettingsByUserId(USER_ID);

            assertThrows(RequestedEntityNotFoundException.class, () -> largeExpenseNotificationService.handleLargeNotification(USER_ID, expense));

            verifyNoInteractions(outboxService);
        }

        @Nested
        class WithExistingSettings {

            @BeforeEach
            void setUp() {
                when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenReturn(settings);
            }

            @Test
            void shouldNotCreateOutboxEventWhenNotificationIsDisabled() {
                settings.setLargeExpenseNotificationEnabled(false);

                largeExpenseNotificationService.handleLargeNotification(USER_ID, expense);

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldNotCreateOutboxEventWhenThresholdIsMissing() {
                settings.setLargeExpenseNotificationThreshold(null);

                largeExpenseNotificationService.handleLargeNotification(USER_ID, expense);

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldNotCreateOutboxEventWhenNotificationIsDisabledAndThresholdIsMissing() {
                settings.setLargeExpenseNotificationEnabled(false);
                settings.setLargeExpenseNotificationThreshold(null);

                largeExpenseNotificationService.handleLargeNotification(USER_ID, expense);

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldNotCreateOutboxEventWhenAmountEqualsThreshold() {
                expense.setAmount(THRESHOLD);

                largeExpenseNotificationService.handleLargeNotification(USER_ID, expense);

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldNotCreateOutboxEventWhenAmountEqualsThresholdWithDifferentScale() {
                expense.setAmount(new BigDecimal("100.0"));

                largeExpenseNotificationService.handleLargeNotification(USER_ID, expense);

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldNotCreateOutboxEventWhenAmountIsBelowThreshold() {
                expense.setAmount(new BigDecimal("99.99"));

                largeExpenseNotificationService.handleLargeNotification(USER_ID, expense);

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenExpenseAmountIsNull() {
                expense.setAmount(null);

                assertThrows(NullPointerException.class, () -> largeExpenseNotificationService.handleLargeNotification(USER_ID, expense));

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenExpenseIdIsNullAndAmountExceedsThreshold() {
                SharedExpense expenseWithoutId = SharedExpense.builder()
                        .ownerId(OWNER_ID)
                        .memberId(MEMBER_ID)
                        .amount(LARGE_AMOUNT)
                        .build();

                assertThrows(NullPointerException.class, () -> largeExpenseNotificationService.handleLargeNotification(USER_ID, expenseWithoutId));

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldCreateOutboxEventWhenAmountExceedsThreshold() {
                largeExpenseNotificationService.handleLargeNotification(USER_ID, expense);

                verify(outboxService).save(eq(EXPENSE_AGGREGATE), eq(EXPENSE_ID.toString()), eq(NOTIFICATION_TOPIC), notificationEventCaptor.capture());

                LargeExpenseNotificationEvent event = notificationEventCaptor.getValue();

                assertEquals(OWNER_ID, event.ownerId());
                assertEquals(MEMBER_ID, event.memberId());
                assertEquals(USER_ID, event.triggeredByUserId());
                assertEquals(EXPENSE_ID, event.expenseId());
                assertEquals(LARGE_AMOUNT, event.amount());
                assertEquals(THRESHOLD, event.threshold());
                assertNotNull(event.occurredAt());
                verifyNoInteractions(sharedAccountParticipantsService);
            }

            @Test
            void shouldCreateOutboxEventWhenAmountIsJustAboveThreshold() {
                expense.setAmount(new BigDecimal("100.01"));

                largeExpenseNotificationService.handleLargeNotification(USER_ID, expense);

                verify(outboxService).save(eq(EXPENSE_AGGREGATE), eq(EXPENSE_ID.toString()), eq(NOTIFICATION_TOPIC), any(LargeExpenseNotificationEvent.class));
            }

            @Test
            void shouldThrowExceptionWhenOutboxServiceFails() {
                doThrow(new IllegalStateException("outbox failed")).when(outboxService).save(anyString(), anyString(), anyString(), any());

                assertThrows(IllegalStateException.class, () -> largeExpenseNotificationService.handleLargeNotification(USER_ID, expense));
            }
        }
    }
}