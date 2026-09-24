package com.finovara.securitymonitoring.accountchange.service;

import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import com.finovara.securitymonitoring.accountchange.config.AccountChangeRiskProperties;
import com.finovara.securitymonitoring.accountchange.model.AccountChangeProfile;
import com.finovara.securitymonitoring.riskengine.dto.RiskContext;
import com.finovara.contracts.securitymonitoring.model.RiskRule;
import com.finovara.contracts.securitymonitoring.dto.TriggeredRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountChangeRiskServiceTest {

    @Mock
    private AccountChangeRiskProperties properties;

    @InjectMocks
    private AccountChangeRiskService accountChangeRiskService;

    private RiskContext context;
    private AccountChangeProfile profile;

    @BeforeEach
    void setUp() {
        context = mock(RiskContext.class);
        profile = mock(AccountChangeProfile.class);
    }

    @Nested
    class Evaluate {

        @Test
        void shouldReturnEmptyListWhenProfileIsNull() {
            when(context.accountChangeProfile()).thenReturn(null);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyListWhenNoRulesTriggered() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.PASSWORD_CHANGED);
            when(profile.getLastPasswordChangeAt()).thenReturn(null);
            when(profile.getCreatedAt()).thenReturn(null);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldTriggerRepeatChangeRuleWhenLastSameChangeWithinWindow() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.PASSWORD_CHANGED);
            when(profile.getLastPasswordChangeAt()).thenReturn(LocalDateTime.now().minusMinutes(2));
            when(profile.getCreatedAt()).thenReturn(null);
            when(properties.getRepeatChangeMinutes()).thenReturn(10L);
            when(properties.getRepeatChangePoints()).thenReturn(15);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rule()).isEqualTo(RiskRule.REPEAT_CHANGE);
            assertThat(result.get(0).points()).isEqualTo(15);
        }

        @Test
        void shouldNotTriggerRepeatChangeRuleWhenLastSameChangeOutsideWindow() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.PASSWORD_CHANGED);
            when(profile.getLastPasswordChangeAt()).thenReturn(LocalDateTime.now().minusMinutes(30));
            when(profile.getCreatedAt()).thenReturn(null);
            when(properties.getRepeatChangeMinutes()).thenReturn(10L);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotTriggerRepeatChangeRuleWhenNoLastChangeOfType() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.EMAIL_CHANGED);
            when(profile.getLastEmailChangeAt()).thenReturn(null);
            when(profile.getCreatedAt()).thenReturn(null);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotTriggerRepeatChangeRuleWhenTriggerTypeHasNoTrackedLastChangeTime() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.LOGIN);
            when(profile.getCreatedAt()).thenReturn(null);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReadLastEmailChangeAtWhenTriggerTypeIsEmailChanged() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.EMAIL_CHANGED);
            when(profile.getLastEmailChangeAt()).thenReturn(LocalDateTime.now().minusMinutes(1));
            when(profile.getCreatedAt()).thenReturn(null);
            when(properties.getRepeatChangeMinutes()).thenReturn(5L);
            when(properties.getRepeatChangePoints()).thenReturn(20);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rule()).isEqualTo(RiskRule.REPEAT_CHANGE);
        }

        @Test
        void shouldReadLastUsernameChangeAtWhenTriggerTypeIsUsernameChanged() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.USERNAME_CHANGED);
            when(profile.getLastUsernameChangeAt()).thenReturn(LocalDateTime.now().minusMinutes(1));
            when(profile.getCreatedAt()).thenReturn(null);
            when(properties.getRepeatChangeMinutes()).thenReturn(5L);
            when(properties.getRepeatChangePoints()).thenReturn(20);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).anyMatch(r -> r.rule() == RiskRule.REPEAT_CHANGE);
        }

        @Test
        void shouldReadLastProfileImageChangeAtWhenTriggerTypeIsProfileImageChanged() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.PROFILE_IMAGE_CHANGED);
            when(profile.getLastProfileImageChangeAt()).thenReturn(LocalDateTime.now().minusMinutes(1));
            when(profile.getCreatedAt()).thenReturn(null);
            when(properties.getRepeatChangeMinutes()).thenReturn(5L);
            when(properties.getRepeatChangePoints()).thenReturn(20);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).anyMatch(r -> r.rule() == RiskRule.REPEAT_CHANGE);
        }

        @Test
        void shouldTriggerIdentityRepaintRuleWhenConditionsMet() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.USERNAME_CHANGED);
            when(profile.getLastUsernameChangeAt()).thenReturn(null);
            when(profile.getLastChangeType()).thenReturn(AccountChangesActivityType.PASSWORD_CHANGED);
            when(profile.getLastChangeAt()).thenReturn(LocalDateTime.now().minusMinutes(3));
            when(profile.getCreatedAt()).thenReturn(null);
            when(properties.getIdentityChangeMinutes()).thenReturn(15L);
            when(properties.getIdentityChangePoints()).thenReturn(25);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rule()).isEqualTo(RiskRule.IDENTITY_REPAINT);
            assertThat(result.get(0).points()).isEqualTo(25);
        }

        @Test
        void shouldTriggerIdentityRepaintRuleWhenLastChangeTypeIsEmailChanged() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.PROFILE_IMAGE_CHANGED);
            when(profile.getLastProfileImageChangeAt()).thenReturn(null);
            when(profile.getLastChangeType()).thenReturn(AccountChangesActivityType.EMAIL_CHANGED);
            when(profile.getLastChangeAt()).thenReturn(LocalDateTime.now().minusMinutes(3));
            when(profile.getCreatedAt()).thenReturn(null);
            when(properties.getIdentityChangeMinutes()).thenReturn(15L);
            when(properties.getIdentityChangePoints()).thenReturn(25);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).anyMatch(r -> r.rule() == RiskRule.IDENTITY_REPAINT);
        }

        @Test
        void shouldNotTriggerIdentityRepaintWhenTriggerTypeIsNotIdentityChange() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.PASSWORD_CHANGED);
            when(profile.getLastPasswordChangeAt()).thenReturn(null);
            when(profile.getCreatedAt()).thenReturn(null);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotTriggerIdentityRepaintWhenLastChangeTypeIsNotCredential() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.USERNAME_CHANGED);
            when(profile.getLastUsernameChangeAt()).thenReturn(null);
            when(profile.getLastChangeType()).thenReturn(AccountChangesActivityType.PROFILE_IMG_CHANGED);
            when(profile.getCreatedAt()).thenReturn(null);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotTriggerIdentityRepaintWhenLastChangeAtIsNull() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.USERNAME_CHANGED);
            when(profile.getLastUsernameChangeAt()).thenReturn(null);
            when(profile.getLastChangeType()).thenReturn(AccountChangesActivityType.PASSWORD_CHANGED);
            when(profile.getLastChangeAt()).thenReturn(null);
            when(profile.getCreatedAt()).thenReturn(null);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotTriggerIdentityRepaintWhenOutsideWindow() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.USERNAME_CHANGED);
            when(profile.getLastUsernameChangeAt()).thenReturn(null);
            when(profile.getLastChangeType()).thenReturn(AccountChangesActivityType.PASSWORD_CHANGED);
            when(profile.getLastChangeAt()).thenReturn(LocalDateTime.now().minusMinutes(60));
            when(profile.getCreatedAt()).thenReturn(null);
            when(properties.getIdentityChangeMinutes()).thenReturn(15L);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldTriggerNewAccountManyChangesRuleWhenConditionsMet() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.PASSWORD_CHANGED);
            when(profile.getLastPasswordChangeAt()).thenReturn(null);
            when(profile.getCreatedAt()).thenReturn(LocalDateTime.now().minusDays(1));
            when(profile.getTotalChangeCount()).thenReturn(10L);
            when(properties.getNewAccountDays()).thenReturn(7L);
            when(properties.getNewAccountChangeCount()).thenReturn(5L);
            when(properties.getNewAccountPoints()).thenReturn(30);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rule()).isEqualTo(RiskRule.NEW_ACCOUNT_MANY_CHANGES);
            assertThat(result.get(0).points()).isEqualTo(30);
        }

        @Test
        void shouldTriggerNewAccountRuleWhenChangeCountExactlyMeetsThreshold() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.PASSWORD_CHANGED);
            when(profile.getLastPasswordChangeAt()).thenReturn(null);
            when(profile.getCreatedAt()).thenReturn(LocalDateTime.now().minusDays(1));
            when(profile.getTotalChangeCount()).thenReturn(5L);
            when(properties.getNewAccountDays()).thenReturn(7L);
            when(properties.getNewAccountChangeCount()).thenReturn(5L);
            when(properties.getNewAccountPoints()).thenReturn(30);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).anyMatch(r -> r.rule() == RiskRule.NEW_ACCOUNT_MANY_CHANGES);
        }

        @Test
        void shouldNotTriggerNewAccountRuleWhenCreatedAtIsNull() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.PASSWORD_CHANGED);
            when(profile.getLastPasswordChangeAt()).thenReturn(null);
            when(profile.getCreatedAt()).thenReturn(null);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotTriggerNewAccountRuleWhenOutsideWindow() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.PASSWORD_CHANGED);
            when(profile.getLastPasswordChangeAt()).thenReturn(null);
            when(profile.getCreatedAt()).thenReturn(LocalDateTime.now().minusDays(30));
            when(properties.getNewAccountDays()).thenReturn(7L);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotTriggerNewAccountRuleWhenChangeCountBelowThreshold() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.PASSWORD_CHANGED);
            when(profile.getLastPasswordChangeAt()).thenReturn(null);
            when(profile.getCreatedAt()).thenReturn(LocalDateTime.now().minusDays(1));
            when(profile.getTotalChangeCount()).thenReturn(2L);
            when(properties.getNewAccountDays()).thenReturn(7L);
            when(properties.getNewAccountChangeCount()).thenReturn(5L);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldTriggerAllThreeRulesWhenAllConditionsMet() {
            when(context.accountChangeProfile()).thenReturn(profile);
            when(context.triggerType()).thenReturn(RiskTriggerType.USERNAME_CHANGED);
            when(profile.getLastUsernameChangeAt()).thenReturn(LocalDateTime.now().minusMinutes(2));
            when(profile.getLastChangeType()).thenReturn(AccountChangesActivityType.PASSWORD_CHANGED);
            when(profile.getLastChangeAt()).thenReturn(LocalDateTime.now().minusMinutes(3));
            when(profile.getCreatedAt()).thenReturn(LocalDateTime.now().minusDays(1));
            when(profile.getTotalChangeCount()).thenReturn(10L);
            when(properties.getRepeatChangeMinutes()).thenReturn(10L);
            when(properties.getRepeatChangePoints()).thenReturn(15);
            when(properties.getIdentityChangeMinutes()).thenReturn(15L);
            when(properties.getIdentityChangePoints()).thenReturn(25);
            when(properties.getNewAccountDays()).thenReturn(7L);
            when(properties.getNewAccountChangeCount()).thenReturn(5L);
            when(properties.getNewAccountPoints()).thenReturn(30);

            List<TriggeredRule> result = accountChangeRiskService.evaluate(context);

            assertThat(result).hasSize(3);
            assertThat(result).extracting(TriggeredRule::rule)
                    .containsExactlyInAnyOrder(RiskRule.REPEAT_CHANGE, RiskRule.IDENTITY_REPAINT, RiskRule.NEW_ACCOUNT_MANY_CHANGES);
        }
    }
}