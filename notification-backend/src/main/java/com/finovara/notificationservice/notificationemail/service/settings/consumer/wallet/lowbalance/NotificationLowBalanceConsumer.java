package com.finovara.notificationservice.notificationemail.service.settings.consumer.wallet.lowbalance;

import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.notification.event.wallet.WalletBalanceChangedEvent;
import com.finovara.notificationservice.feignclient.AuthBackendClient;
import com.finovara.notificationservice.notificationemail.model.ActionEmailNotificationType;
import com.finovara.notificationservice.notificationemail.model.NotificationEmailSettings;
import com.finovara.notificationservice.notificationemail.repository.NotificationEmailSettingsRepository;
import com.finovara.notificationservice.notificationemail.service.EmailNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationLowBalanceConsumer {

    private final NotificationEmailSettingsRepository notificationEmailSettingsRepository;
    private final EmailNotifier emailNotifier;
    private final AuthBackendClient authBackendClient;

    @KafkaListener(topics = "wallet.balance-changed", groupId = "notification-email-service")
    public void handleWalletBalanceChanged(WalletBalanceChangedEvent event) {
        notificationEmailSettingsRepository.findByUserId(event.userId())
                .filter(NotificationEmailSettings::isNotifyOnWalletLowBalance)
                .filter(settings -> settings.getWalletLowBalanceThreshold() != null)
                .filter(settings -> crossedBelowThreshold(event, settings))
                .ifPresent(settings -> notifyLowBalance(event, settings));
    }

    private boolean crossedBelowThreshold(WalletBalanceChangedEvent event, NotificationEmailSettings settings) {
        BigDecimal threshold = settings.getWalletLowBalanceThreshold();
        return event.previousBalance().compareTo(threshold) >= 0
                && event.currentBalance().compareTo(threshold) < 0;
    }

    private void notifyLowBalance(WalletBalanceChangedEvent event, NotificationEmailSettings settings) {
        UserDataResponse userDataResponse = authBackendClient.getUserData(event.userId());
        if (userDataResponse.email().isEmpty()) {
            log.warn("Skipping low balance notification - no email found for userId={}", event.userId());
            return;
        }

        BigDecimal dropped = event.previousBalance().subtract(event.currentBalance()).setScale(2, RoundingMode.HALF_UP);
        Map<String, String>  placeholders =  buildPlaceHolders(event, settings, userDataResponse, dropped);

        emailNotifier.send(ActionEmailNotificationType.WALLET_LOW_BALANCE, userDataResponse.email().get(), placeholders);

        log.info("Sent wallet low balance notification for userId={}, dropped={}", event.userId(), dropped);
    }

    private Map<String, String> buildPlaceHolders(WalletBalanceChangedEvent event, NotificationEmailSettings settings, UserDataResponse userDataResponse, BigDecimal dropped) {
        return Map.ofEntries(
                Map.entry("username", userDataResponse.username().orElse("Użytkowniku")),
                Map.entry("amountDropped", dropped.toPlainString()),
                Map.entry("currentBalance", event.currentBalance().setScale(2, RoundingMode.HALF_UP).toPlainString()),
                Map.entry("threshold", settings.getWalletLowBalanceThreshold().setScale(2, RoundingMode.HALF_UP).toPlainString())
        );
    }

}

