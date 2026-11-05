package com.finovara.notificationservice.notificationemail.service.settings.action.wallet.lowbalance.dto;

import com.finovara.notificationservice.notificationemail.model.EmailNotificationSettingRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record WalletLowBalanceDto(
        Boolean enabled,
        String authorizationCode,
        @DecimalMin("1") @DecimalMax("999999")
        BigDecimal amountThreshold
) implements EmailNotificationSettingRequest {
}