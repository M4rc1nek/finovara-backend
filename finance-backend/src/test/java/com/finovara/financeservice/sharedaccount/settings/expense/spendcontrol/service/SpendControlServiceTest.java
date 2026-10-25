package com.finovara.financeservice.sharedaccount.settings.expense.spendcontrol.service;

import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.expense.spendcontrol.dto.SpendControlDto;
import com.finovara.financeservice.util.sharedaccount.settings.SharedSettingsManager;
import com.finovara.financeservice.util.sharedaccount.wallet.SharedWalletManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpendControlServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private SharedSettingsManager sharedSettingsManager;

    @Mock
    private SharedWalletManager sharedWalletManager;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private SpendControlService spendControlService;

    private SharedAccountParticipantsResponse participantsResponse;

    @BeforeEach
    void setUp() {
        participantsResponse = mock(SharedAccountParticipantsResponse.class);
    }

    @Nested
    class SaveSpendControlService {

        @Test
        void shouldUpdateSettingsAndPublishEventWhenEnabledFlagChanges() {
            SharedAccountSettings settings = SharedAccountSettings.builder()
                    .spendControlEnabled(false)
                    .spendControlPercentage(new BigDecimal("50"))
                    .build();
            SpendControlDto dto = new SpendControlDto(true, new BigDecimal("50"));
            when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenReturn(settings);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);

            spendControlService.saveSpendControlService(USER_ID, dto);

            assertThat(settings.isSpendControlEnabled()).isTrue();
            verify(outboxService).save(eq("SharedAccountSettings"), eq(USER_ID.toString()), eq("shared-account.activity"), any(SharedAccountActivityLogEvent.class));
        }

        @Test
        void shouldUpdateSettingsAndPublishEventWhenPercentageChanges() {
            SharedAccountSettings settings = SharedAccountSettings.builder()
                    .spendControlEnabled(true)
                    .spendControlPercentage(new BigDecimal("50"))
                    .build();
            SpendControlDto dto = new SpendControlDto(true, new BigDecimal("75"));
            when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenReturn(settings);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);

            spendControlService.saveSpendControlService(USER_ID, dto);

            assertThat(settings.getSpendControlPercentage()).isEqualByComparingTo(new BigDecimal("75"));
            verify(outboxService).save(eq("SharedAccountSettings"), eq(USER_ID.toString()), eq("shared-account.activity"), any(SharedAccountActivityLogEvent.class));
        }

        @Test
        void shouldNotUpdateSettingsWhenNothingChanged() {
            SharedAccountSettings settings = SharedAccountSettings.builder()
                    .spendControlEnabled(true)
                    .spendControlPercentage(new BigDecimal("50"))
                    .build();
            SpendControlDto dto = new SpendControlDto(true, new BigDecimal("50"));
            when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenReturn(settings);

            spendControlService.saveSpendControlService(USER_ID, dto);

            assertThat(settings.isSpendControlEnabled()).isTrue();
            assertThat(settings.getSpendControlPercentage()).isEqualByComparingTo(new BigDecimal("50"));
        }

        @Test
        void shouldNotPublishEventWhenNothingChanged() {
            SharedAccountSettings settings = SharedAccountSettings.builder()
                    .spendControlEnabled(true)
                    .spendControlPercentage(new BigDecimal("50"))
                    .build();
            SpendControlDto dto = new SpendControlDto(true, new BigDecimal("50"));
            when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenReturn(settings);

            spendControlService.saveSpendControlService(USER_ID, dto);

            verifyNoInteractions(outboxService, sharedAccountParticipantsService);
        }

        @Test
        void shouldThrowExceptionWhenSettingsRepositoryFails() {
            SpendControlDto dto = new SpendControlDto(true, new BigDecimal("50"));
            when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenThrow(new IllegalStateException("query failed"));

            assertThrows(IllegalStateException.class, () -> spendControlService.saveSpendControlService(USER_ID, dto));

            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsServiceFailsAfterChange() {
            SharedAccountSettings settings = SharedAccountSettings.builder()
                    .spendControlEnabled(false)
                    .spendControlPercentage(new BigDecimal("50"))
                    .build();
            SpendControlDto dto = new SpendControlDto(true, new BigDecimal("50"));
            when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenReturn(settings);
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenThrow(new IllegalStateException("participants failed"));

            assertThrows(IllegalStateException.class, () -> spendControlService.saveSpendControlService(USER_ID, dto));

            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class GetSmartScan {

        @Test
        void shouldReturnCurrentSpendControlSettings() {
            SharedAccountSettings settings = SharedAccountSettings.builder()
                    .spendControlEnabled(true)
                    .spendControlPercentage(new BigDecimal("40"))
                    .build();
            when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenReturn(settings);

            SpendControlDto result = spendControlService.getSmartScan(USER_ID);

            assertThat(result.spendControlEnabled()).isTrue();
            assertThat(result.spendControlPercentage()).isEqualByComparingTo(new BigDecimal("40"));
        }

        @Test
        void shouldReturnDisabledSettingsWhenSpendControlNotEnabled() {
            SharedAccountSettings settings = SharedAccountSettings.builder()
                    .spendControlEnabled(false)
                    .spendControlPercentage(BigDecimal.ZERO)
                    .build();
            when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenReturn(settings);

            SpendControlDto result = spendControlService.getSmartScan(USER_ID);

            assertThat(result.spendControlEnabled()).isFalse();
        }

        @Test
        void shouldThrowExceptionWhenSettingsRepositoryFails() {
            when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenThrow(new IllegalStateException("query failed"));

            assertThrows(IllegalStateException.class, () -> spendControlService.getSmartScan(USER_ID));
        }
    }

    @Nested
    class HandleSpendControl {

        @Test
        void shouldDoNothingWhenSpendControlDisabled() {
            SharedAccountSettings settings = SharedAccountSettings.builder()
                    .spendControlEnabled(false)
                    .spendControlPercentage(new BigDecimal("50"))
                    .build();
            when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenReturn(settings);

            spendControlService.handleSpendControl(USER_ID, new BigDecimal("10000.00"));

            verifyNoInteractions(sharedWalletManager);
        }

        @Test
        void shouldThrowExceptionWhenSettingsRepositoryFails() {
            when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenThrow(new IllegalStateException("query failed"));

            assertThrows(IllegalStateException.class, () -> spendControlService.handleSpendControl(USER_ID, new BigDecimal("100.00")));

            verifyNoInteractions(sharedWalletManager);
        }

        @Test
        void shouldThrowExceptionWhenWalletRepositoryFails() {
            SharedAccountSettings settings = SharedAccountSettings.builder()
                    .spendControlEnabled(true)
                    .spendControlPercentage(new BigDecimal("50"))
                    .build();
            when(sharedSettingsManager.getSharedAccountSettingsByUserId(USER_ID)).thenReturn(settings);
            when(sharedWalletManager.getWalletForUpdateOrThrow(USER_ID)).thenThrow(new IllegalStateException("wallet query failed"));

            assertThrows(IllegalStateException.class, () -> spendControlService.handleSpendControl(USER_ID, new BigDecimal("100.00")));
        }
    }
}