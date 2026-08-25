package com.finovara.notificationservice.notificationemail.service.settings.consumer.wallet.lowbalance;

import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.notification.event.wallet.WalletBalanceChangedEvent;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.model.ActionEmailNotificationType;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.service.EmailNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationLowBalanceConsumerTest {

    private static final Long USER_ID = 7L;
    private static final String RECIPIENT_EMAIL = "user@example.com";
    private static final String USERNAME = "kasia";

    @Mock
    private NotificationEmailSettingsRepository notificationEmailSettingsRepository;

    @Mock
    private EmailNotifier emailNotifier;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private NotificationEmailSettings settings;

    private NotificationLowBalanceConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationLowBalanceConsumer(notificationEmailSettingsRepository, emailNotifier, authBackendClient);
    }

    @Nested
    class NoNotificationCases {

        @Test
        void shouldDoNothingWhenSettingsNotFound() {
            WalletBalanceChangedEvent event = new WalletBalanceChangedEvent(USER_ID, BigDecimal.valueOf(300), BigDecimal.valueOf(170), LocalDateTime.now());
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            consumer.handleWalletBalanceChanged(event);

            verify(emailNotifier, never()).send(any(), any(), any());
        }

        @Test
        void shouldDoNothingWhenNotificationDisabled() {
            WalletBalanceChangedEvent event = new WalletBalanceChangedEvent(USER_ID, BigDecimal.valueOf(300), BigDecimal.valueOf(170), LocalDateTime.now());
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
            when(settings.isNotifyOnWalletLowBalance()).thenReturn(false);

            consumer.handleWalletBalanceChanged(event);

            verify(emailNotifier, never()).send(any(), any(), any());
        }

        @Test
        void shouldDoNothingWhenThresholdIsNull() {
            WalletBalanceChangedEvent event = new WalletBalanceChangedEvent(USER_ID, BigDecimal.valueOf(300), BigDecimal.valueOf(170), LocalDateTime.now());
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
            when(settings.isNotifyOnWalletLowBalance()).thenReturn(true);
            when(settings.getWalletLowBalanceThreshold()).thenReturn(null);

            consumer.handleWalletBalanceChanged(event);

            verify(emailNotifier, never()).send(any(), any(), any());
        }

        @Test
        void shouldDoNothingWhenBalanceWasAlreadyBelowThreshold() {
            WalletBalanceChangedEvent event = new WalletBalanceChangedEvent(USER_ID, BigDecimal.valueOf(150), BigDecimal.valueOf(100), LocalDateTime.now());
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
            when(settings.isNotifyOnWalletLowBalance()).thenReturn(true);
            when(settings.getWalletLowBalanceThreshold()).thenReturn(BigDecimal.valueOf(300));

            consumer.handleWalletBalanceChanged(event);

            verify(emailNotifier, never()).send(any(), any(), any());
        }

        @Test
        void shouldDoNothingWhenUserEmailIsMissing() {
            WalletBalanceChangedEvent event = new WalletBalanceChangedEvent(USER_ID, BigDecimal.valueOf(300), BigDecimal.valueOf(170), LocalDateTime.now());
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
            when(settings.isNotifyOnWalletLowBalance()).thenReturn(true);
            when(settings.getWalletLowBalanceThreshold()).thenReturn(BigDecimal.valueOf(300));
            when(authBackendClient.getUserData(USER_ID)).thenReturn(new UserDataResponse(USER_ID, Optional.of(USERNAME), Optional.empty()));

            consumer.handleWalletBalanceChanged(event);

            verify(emailNotifier, never()).send(any(), any(), any());
        }
    }

    @Nested
    class NotificationSentCases {

        @Test
        void shouldSendEmailWhenBalanceCrossesBelowThreshold() {
            WalletBalanceChangedEvent event = new WalletBalanceChangedEvent(USER_ID, BigDecimal.valueOf(300), BigDecimal.valueOf(170), LocalDateTime.now());
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
            when(settings.isNotifyOnWalletLowBalance()).thenReturn(true);
            when(settings.getWalletLowBalanceThreshold()).thenReturn(BigDecimal.valueOf(300));
            when(authBackendClient.getUserData(USER_ID)).thenReturn(new UserDataResponse(USER_ID, Optional.of(USERNAME), Optional.of(RECIPIENT_EMAIL)));

            consumer.handleWalletBalanceChanged(event);

            verify(emailNotifier).send(eq(ActionEmailNotificationType.WALLET_LOW_BALANCE), eq(RECIPIENT_EMAIL), any());
        }

        @Test
        void shouldSendEmailWhenPreviousBalanceExactlyEqualsThreshold() {
            WalletBalanceChangedEvent event = new WalletBalanceChangedEvent(USER_ID, BigDecimal.valueOf(300), BigDecimal.valueOf(299), LocalDateTime.now());
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
            when(settings.isNotifyOnWalletLowBalance()).thenReturn(true);
            when(settings.getWalletLowBalanceThreshold()).thenReturn(BigDecimal.valueOf(300));
            when(authBackendClient.getUserData(USER_ID)).thenReturn(new UserDataResponse(USER_ID, Optional.of(USERNAME), Optional.of(RECIPIENT_EMAIL)));

            consumer.handleWalletBalanceChanged(event);

            verify(emailNotifier).send(eq(ActionEmailNotificationType.WALLET_LOW_BALANCE), eq(RECIPIENT_EMAIL), any());
        }

        @Test
        void shouldCalculateCorrectDroppedAmountInPlaceholders() {
            WalletBalanceChangedEvent event = new WalletBalanceChangedEvent(USER_ID, BigDecimal.valueOf(300), BigDecimal.valueOf(170), LocalDateTime.now());
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
            when(settings.isNotifyOnWalletLowBalance()).thenReturn(true);
            when(settings.getWalletLowBalanceThreshold()).thenReturn(BigDecimal.valueOf(300));
            when(authBackendClient.getUserData(USER_ID)).thenReturn(new UserDataResponse(USER_ID, Optional.of(USERNAME), Optional.of(RECIPIENT_EMAIL)));

            ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);

            consumer.handleWalletBalanceChanged(event);

            verify(emailNotifier).send(any(), any(), captor.capture());
            assertEquals("130.00", captor.getValue().get("amountDropped"));
            assertEquals("170.00", captor.getValue().get("currentBalance"));
            assertEquals("300.00", captor.getValue().get("threshold"));
            assertEquals(USERNAME, captor.getValue().get("username"));
        }

        @Test
        void shouldUseDefaultUsernameWhenMissing() {
            WalletBalanceChangedEvent event = new WalletBalanceChangedEvent(USER_ID, BigDecimal.valueOf(300), BigDecimal.valueOf(170), LocalDateTime.now());
            when(notificationEmailSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
            when(settings.isNotifyOnWalletLowBalance()).thenReturn(true);
            when(settings.getWalletLowBalanceThreshold()).thenReturn(BigDecimal.valueOf(300));
            when(authBackendClient.getUserData(USER_ID)).thenReturn(new UserDataResponse(USER_ID, Optional.empty(), Optional.of(RECIPIENT_EMAIL)));

            ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);

            consumer.handleWalletBalanceChanged(event);

            verify(emailNotifier).send(any(), eq(RECIPIENT_EMAIL), captor.capture());
            assertEquals("Użytkowniku", captor.getValue().get("username"));
        }
    }
}