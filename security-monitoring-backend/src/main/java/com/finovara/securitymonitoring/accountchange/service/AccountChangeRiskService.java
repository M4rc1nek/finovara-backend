package com.finovara.securitymonitoring.accountchange.service;

import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.securitymonitoring.accountchange.config.AccountChangeRiskProperties;
import com.finovara.securitymonitoring.accountchange.model.AccountChangeProfile;
import com.finovara.securitymonitoring.riskengine.dto.RiskContext;
import com.finovara.securitymonitoring.riskengine.model.RiskRule;
import com.finovara.securitymonitoring.riskengine.model.RiskTriggerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountChangeRiskService {

    private final AccountChangeRiskProperties properties;

    public int evaluate(RiskContext context) {
        AccountChangeProfile profile = context.accountChangeProfile();
        if (profile == null) {
            return 0;
        }

        log.info("Checking account change risk for userId={}", context.userId());

        int totalPoints = addPoints(context, RiskRule.REPEAT_CHANGE, isRepeatChange(context, profile), properties.getRepeatChangePoints())
                + addPoints(context, RiskRule.IDENTITY_REPAINT, isIdentityRepaint(context, profile), properties.getIdentityChangePoints())
                + addPoints(context, RiskRule.NEW_ACCOUNT_MANY_CHANGES, isNewAccountWithManyChanges(profile), properties.getNewAccountPoints());

        log.info("Account change risk points for userId={} is {}", context.userId(), totalPoints);
        return totalPoints;
    }

    private boolean isRepeatChange(RiskContext context, AccountChangeProfile profile) {
        LocalDateTime lastSameChangeAt = lastChangeTimeForType(profile, context.triggerType());
        return lastSameChangeAt != null && isWithin(lastSameChangeAt, Duration.ofMinutes(properties.getRepeatChangeMinutes()));
    }

    private boolean isIdentityRepaint(RiskContext context, AccountChangeProfile profile) {
        return isIdentityChange(context.triggerType())
                && isCredentialType(profile.getLastChangeType())
                && profile.getLastChangeAt() != null
                && isWithin(profile.getLastChangeAt(), Duration.ofMinutes(properties.getIdentityChangeMinutes()));
    }

    private boolean isNewAccountWithManyChanges(AccountChangeProfile profile) {
        return profile.getCreatedAt() != null
                && isWithin(profile.getCreatedAt(), Duration.ofDays(properties.getNewAccountDays()))
                && profile.getTotalChangeCount() >= properties.getNewAccountChangeCount();
    }

    private LocalDateTime lastChangeTimeForType(AccountChangeProfile profile, RiskTriggerType type) {
        return switch (type) {
            case PASSWORD_CHANGED -> profile.getLastPasswordChangeAt();
            case EMAIL_CHANGED -> profile.getLastEmailChangeAt();
            case USERNAME_CHANGED -> profile.getLastUsernameChangeAt();
            case PROFILE_IMAGE_CHANGED -> profile.getLastProfileImageChangeAt();
            default -> null;
        };
    }

    private boolean isIdentityChange(RiskTriggerType type) {
        return type == RiskTriggerType.USERNAME_CHANGED || type == RiskTriggerType.PROFILE_IMAGE_CHANGED;
    }

    private boolean isCredentialType(AccountChangesActivityType type) {
        return type == AccountChangesActivityType.PASSWORD_CHANGED || type == AccountChangesActivityType.EMAIL_CHANGED;
    }

    private boolean isWithin(LocalDateTime since, Duration window) {
        return Duration.between(since, LocalDateTime.now()).compareTo(window) < 0;
    }

    private int addPoints(RiskContext context, RiskRule riskRule, boolean triggered, int points) {
        if (triggered) {
            log.info("AccountChange Risk: Rule triggered for userId={}: {} (+{} points)", context.userId(), riskRule, points);
        }
        return triggered ? points : 0;
    }
}