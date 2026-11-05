package com.finovara.notificationservice.notificationemail.service.wallet.lowbalance;

import com.finovara.contracts.user.authorization.dto.UserDataResponse;
import com.finovara.contracts.mainaccount.transaction.event.wallet.WalletBalanceChangedEvent;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.model.ActionEmailNotificationType;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.settings.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.util.EmailNotifier;
import org.junit.jupiter.api.BeforeEach;
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
class LowBalanceNotificationHandlerTest {

    private static final Long USER_ID = 7L;
    private static final String RECIPIENT_EMAIL = "user@example.com";
    private static final String USERNAME = "kasia";

    @Mock
    private NotificationEmailSettingsRepository settingsRepository;

    @Mock
    private EmailNotifier emailNotifier;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private NotificationEmailSettings settings;

    private LowBalanceNotificationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LowBalanceNotificationHandler(settingsRepository, emailNotifier, authBackendClient);
    }

    @Test
    void shouldDoNothingWhenSettingsNotFound() {
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        handler.handle(balanceChanged(300, 170));

        verifyNoInteractions(emailNotifier, authBackendClient);
    }

    @Test
    void shouldDoNothingWhenNotificationIsDisabled() {
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        when(settings.isNotifyOnWalletLowBalance()).thenReturn(false);

        handler.handle(balanceChanged(300, 170));

        verifyNoInteractions(emailNotifier, authBackendClient);
    }

    @Test
    void shouldDoNothingWhenThresholdIsNull() {
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        when(settings.isNotifyOnWalletLowBalance()).thenReturn(true);
        when(settings.getWalletLowBalanceThreshold()).thenReturn(null);

        handler.handle(balanceChanged(300, 170));

        verifyNoInteractions(emailNotifier, authBackendClient);
    }

    @Test
    void shouldDoNothingWhenBalanceWasAlreadyBelowThreshold() {
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        when(settings.isNotifyOnWalletLowBalance()).thenReturn(true);
        when(settings.getWalletLowBalanceThreshold()).thenReturn(BigDecimal.valueOf(300));

        handler.handle(balanceChanged(150, 100));

        verifyNoInteractions(emailNotifier, authBackendClient);
    }

    @Test
    void shouldDoNothingWhenUserEmailIsMissing() {
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        when(settings.isNotifyOnWalletLowBalance()).thenReturn(true);
        when(settings.getWalletLowBalanceThreshold()).thenReturn(BigDecimal.valueOf(300));
        when(authBackendClient.getUserData(USER_ID)).thenReturn(
                new UserDataResponse(USER_ID, Optional.of(USERNAME), Optional.empty()));

        handler.handle(balanceChanged(300, 170));

        verify(emailNotifier, never()).send(any(), any(), any());
    }

    @Test
    void shouldSendEmailWhenBalanceCrossesBelowThreshold() {
        givenEnabledSettings(BigDecimal.valueOf(300));

        handler.handle(balanceChanged(300, 170));

        verify(emailNotifier).send(eq(ActionEmailNotificationType.WALLET_LOW_BALANCE), eq(RECIPIENT_EMAIL), any());
    }

    @Test
    void shouldSendWhenPreviousBalanceExactlyEqualsThreshold() {
        givenEnabledSettings(BigDecimal.valueOf(300));

        handler.handle(balanceChanged(300, 299));

        verify(emailNotifier).send(eq(ActionEmailNotificationType.WALLET_LOW_BALANCE), eq(RECIPIENT_EMAIL), any());
    }

    @Test
    void shouldCalculateCorrectValuesInPlaceholders() {
        givenEnabledSettings(BigDecimal.valueOf(300));
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);

        handler.handle(balanceChanged(300, 170));

        verify(emailNotifier).send(any(), any(), captor.capture());
        assertEquals("130.00", captor.getValue().get("amountDropped"));
        assertEquals("170.00", captor.getValue().get("currentBalance"));
        assertEquals("300.00", captor.getValue().get("threshold"));
        assertEquals(USERNAME, captor.getValue().get("username"));
    }

    @Test
    void shouldUseDefaultUsernameWhenMissing() {
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        when(settings.isNotifyOnWalletLowBalance()).thenReturn(true);
        when(settings.getWalletLowBalanceThreshold()).thenReturn(BigDecimal.valueOf(300));
        when(authBackendClient.getUserData(USER_ID)).thenReturn(
                new UserDataResponse(USER_ID, Optional.empty(), Optional.of(RECIPIENT_EMAIL)));
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);

        handler.handle(balanceChanged(300, 170));

        verify(emailNotifier).send(any(), eq(RECIPIENT_EMAIL), captor.capture());
        assertEquals("Użytkowniku", captor.getValue().get("username"));
    }

    private void givenEnabledSettings(BigDecimal threshold) {
        when(settingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings));
        when(settings.isNotifyOnWalletLowBalance()).thenReturn(true);
        when(settings.getWalletLowBalanceThreshold()).thenReturn(threshold);
        when(authBackendClient.getUserData(USER_ID)).thenReturn(
                new UserDataResponse(USER_ID, Optional.of(USERNAME), Optional.of(RECIPIENT_EMAIL)));
    }

    private WalletBalanceChangedEvent balanceChanged(long previous, long current) {
        return new WalletBalanceChangedEvent(USER_ID, BigDecimal.valueOf(previous), BigDecimal.valueOf(current), LocalDateTime.now());
    }
}
