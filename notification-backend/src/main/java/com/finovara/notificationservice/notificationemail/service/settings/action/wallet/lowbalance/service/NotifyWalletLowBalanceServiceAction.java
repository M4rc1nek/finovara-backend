package com.finovara.notificationservice.notificationemail.service.settings.action.wallet.lowbalance.service;

import com.finovara.contracts.activity.event.settings.SettingsActivityEvent;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.service.settings.action.core.AbstractActionNotificationEmailService;
import com.finovara.notificationservice.notificationemail.service.settings.action.wallet.lowbalance.dto.WalletLowBalanceDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
@Service
public class NotifyWalletLowBalanceServiceAction extends AbstractActionNotificationEmailService<WalletLowBalanceDto, WalletLowBalanceDto> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotifyWalletLowBalanceServiceAction(NotificationEmailSettingsRepository repository,
                                               AuthBackendClient authBackendClient,
                                               KafkaTemplate<String, Object> kafkaTemplate,
                                               AdditionalAuthorizationCodeResolver resolver) {
        super(repository, authBackendClient, resolver);
        this.kafkaTemplate = kafkaTemplate;
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
        kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.NOTIFICATION_WALLET_LOW_BALANCE,
                enabled ? SettingActivityStatus.ENABLED : SettingActivityStatus.DISABLED,
                LocalDateTime.now()));
    }
}