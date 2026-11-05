package com.finovara.notificationservice.notificationemail.settings.action.wallet.lowbalance.service;

import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.mainaccount.activity.model.SettingType;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.settings.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.settings.action.core.AbstractActionNotificationEmailService;
import com.finovara.notificationservice.notificationemail.settings.action.wallet.lowbalance.dto.WalletLowBalanceDto;
import org.springframework.stereotype.Service;

@Service
public class NotifyWalletLowBalanceServiceAction extends AbstractActionNotificationEmailService<WalletLowBalanceDto, WalletLowBalanceDto> {

    public NotifyWalletLowBalanceServiceAction(NotificationEmailSettingsRepository repository,
                                               AuthBackendClient authBackendClient,
                                               OutboxService outboxService,
                                               AdditionalAuthorizationCodeResolver resolver) {
        super(repository, authBackendClient, resolver, outboxService);
    }

    @Override
    protected void applySetting(NotificationEmailSettings settings, WalletLowBalanceDto dto) {
        settings.setNotifyOnWalletLowBalance(Boolean.TRUE.equals(dto.enabled()));
        settings.setWalletLowBalanceThreshold(dto.amountThreshold());
    }

    @Override
    protected WalletLowBalanceDto mapToDto(NotificationEmailSettings settings) {
        return new WalletLowBalanceDto(settings.isNotifyOnWalletLowBalance(), null, settings.getWalletLowBalanceThreshold());
    }

    @Override
    protected void handleActivity(Long userId, boolean enabled) {
        publishActivity(userId, SettingType.NOTIFICATION_WALLET_LOW_BALANCE, enabled);
    }
}
