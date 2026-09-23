package com.finovara.securitymonitoring.riskengine.service;

import com.finovara.contracts.securitymonitoring.dto.RiskAction;
import com.finovara.contracts.securitymonitoring.dto.RiskEvaluationRequest;
import com.finovara.contracts.securitymonitoring.dto.RiskEvaluationResponse;
import com.finovara.contracts.securitymonitoring.dto.RiskTriggerType;
import com.finovara.securitymonitoring.accountchange.model.AccountChangeProfile;
import com.finovara.securitymonitoring.accountchange.repository.AccountChangeProfileRepository;
import com.finovara.securitymonitoring.accountchange.service.AccountChangeRiskService;
import com.finovara.securitymonitoring.login.model.LoginProfile;
import com.finovara.securitymonitoring.login.repository.LoginProfileRepository;
import com.finovara.securitymonitoring.login.service.LoginRiskService;
import com.finovara.securitymonitoring.riskchallenge.service.RiskChallengeService;
import com.finovara.securitymonitoring.riskengine.config.RiskActionThresholds;
import com.finovara.securitymonitoring.riskengine.dto.RiskContext;
import com.finovara.securitymonitoring.riskengine.model.RiskOperation;
import com.finovara.securitymonitoring.riskengine.model.RiskRule;
import com.finovara.securitymonitoring.riskengine.model.RiskRuleCollection;
import com.finovara.securitymonitoring.riskengine.model.TriggeredRule;
import com.finovara.securitymonitoring.riskengine.repository.RiskOperationRepository;
import com.finovara.securitymonitoring.transaction.model.TransactionProfile;
import com.finovara.securitymonitoring.transaction.repository.TransactionProfileRepository;
import com.finovara.securitymonitoring.transaction.service.TransactionRiskService;
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
import java.util.List;
import java.util.Optional;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskEngineServiceTest {

    private static final String SOURCE_EVENT_ID = "evt-123";
    private static final Long USER_ID = 1L;
    private static final BigDecimal AMOUNT = new BigDecimal("500.00");
    private static final String CATEGORY = "GROCERIES";
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final String LOCATION = "Warsaw";
    private static final String BROWSER = "Chrome";
    private static final String EMAIL = "user@example.com";
    private static final int LOGIN_LOG_ONLY_MAX_POINTS = 20;
    private static final int SOFT_CHALLENGE_POINTS = 30;
    private static final int AUTHORIZATION_POINTS = 60;
    private static final int FULL_VERIFICATION_POINTS = 90;

    @Mock
    private TransactionRiskService transactionRiskService;

    @Mock
    private LoginRiskService loginRiskService;

    @Mock
    private AccountChangeRiskService accountChangeRiskService;

    @Mock
    private TransactionProfileRepository transactionProfileRepository;

    @Mock
    private LoginProfileRepository loginProfileRepository;

    @Mock
    private AccountChangeProfileRepository accountChangeProfileRepository;

    @Mock
    private RiskOperationRepository riskOperationRepository;

    @Mock
    private RiskActionThresholds thresholds;

    @Mock
    private RiskChallengeService riskChallengeService;

    @Mock
    private TransactionProfile transactionProfile;

    @Mock
    private LoginProfile loginProfile;

    @Mock
    private AccountChangeProfile accountChangeProfile;

    @Captor
    private ArgumentCaptor<RiskOperation> operationCaptor;

    @Captor
    private ArgumentCaptor<RiskContext> contextCaptor;

    @InjectMocks
    private RiskEngineService riskEngineService;

    private RiskEvaluationRequest requestWithTriggerType(RiskTriggerType triggerType) {
        return new RiskEvaluationRequest(
                USER_ID,
                triggerType,
                SOURCE_EVENT_ID,
                AMOUNT,
                CATEGORY,
                IP_ADDRESS,
                LOCATION,
                BROWSER,
                EMAIL
        );
    }

    private void stubEmptyProfiles() {
        when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(loginProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(accountChangeProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
    }

    private void stubNoRiskRulesTriggered() {
        when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
        when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
        when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
    }

    @Nested
    class EvaluateWhenOperationAlreadyExists {

        @Test
        void shouldReturnExistingOperationResponseWhenSourceEventIdAlreadyProcessed() {
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);
            RiskOperation existingOperation = RiskOperation.builder()
                    .id(99L)
                    .sourceEventId(SOURCE_EVENT_ID)
                    .triggerType(RiskTriggerType.EXPENSE)
                    .score(45)
                    .action(RiskAction.SOFT_CHALLENGE)
                    .passwordConfirmed(true)
                    .emailCodeConfirmed(false)
                    .userId(USER_ID)
                    .build();
            when(riskOperationRepository.findBySourceEventId(SOURCE_EVENT_ID)).thenReturn(Optional.of(existingOperation));

            RiskEvaluationResponse response = riskEngineService.evaluate(request);

            assertEquals(99L, response.riskOperationId());
            assertEquals(45, response.score());
            assertEquals(RiskAction.SOFT_CHALLENGE, response.action());
            assertTrue(response.passwordRequired());
            assertFalse(response.emailCodeRequired());
            assertTrue(response.isFullyVerified());
        }

        @Test
        void shouldNotPersistNewOperationWhenSourceEventIdAlreadyProcessed() {
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);
            RiskOperation existingOperation = RiskOperation.builder()
                    .id(1L)
                    .sourceEventId(SOURCE_EVENT_ID)
                    .triggerType(RiskTriggerType.EXPENSE)
                    .score(10)
                    .action(RiskAction.LOG_ONLY)
                    .userId(USER_ID)
                    .build();
            when(riskOperationRepository.findBySourceEventId(SOURCE_EVENT_ID)).thenReturn(Optional.of(existingOperation));

            riskEngineService.evaluate(request);

            verify(riskOperationRepository, never()).save(any(RiskOperation.class));
        }

        @Test
        void shouldNotEvaluateRiskRulesWhenSourceEventIdAlreadyProcessed() {
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);
            RiskOperation existingOperation = RiskOperation.builder()
                    .id(1L)
                    .sourceEventId(SOURCE_EVENT_ID)
                    .triggerType(RiskTriggerType.EXPENSE)
                    .score(10)
                    .action(RiskAction.LOG_ONLY)
                    .userId(USER_ID)
                    .build();
            when(riskOperationRepository.findBySourceEventId(SOURCE_EVENT_ID)).thenReturn(Optional.of(existingOperation));

            riskEngineService.evaluate(request);

            verifyNoInteractions(transactionRiskService, loginRiskService, accountChangeRiskService, riskChallengeService);
        }
    }

    @Nested
    class EvaluateActionResolution {

        @BeforeEach
        void setUp() {
            when(riskOperationRepository.findBySourceEventId(SOURCE_EVENT_ID)).thenReturn(Optional.empty());
            stubEmptyProfiles();
        }

        @Test
        void shouldResolveLogOnlyWhenLoginScoreIsBelowThreshold() {
            when(thresholds.getLoginLogOnlyMaxPoints()).thenReturn(LOGIN_LOG_ONLY_MAX_POINTS);
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.UNKNOWN_DEVICE, LOGIN_LOG_ONLY_MAX_POINTS)));
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.LOGIN);

            RiskEvaluationResponse response = riskEngineService.evaluate(request);

            assertEquals(RiskAction.LOG_ONLY, response.action());
        }

        @Test
        void shouldResolveAuthorizationRequiredWhenLoginScoreExceedsThreshold() {
            when(thresholds.getLoginLogOnlyMaxPoints()).thenReturn(LOGIN_LOG_ONLY_MAX_POINTS);
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.IMPOSSIBLE_TRAVEL, LOGIN_LOG_ONLY_MAX_POINTS + 1)));
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.LOGIN);

            RiskEvaluationResponse response = riskEngineService.evaluate(request);

            assertEquals(RiskAction.AUTHORIZATION_REQUIRED, response.action());
        }

        @Test
        void shouldResolveFullVerificationRequiredWhenNonLoginScoreReachesFullVerificationThreshold() {
            when(thresholds.getFullVerificationPoints()).thenReturn(FULL_VERIFICATION_POINTS);
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.EXPENSE_HIGH, FULL_VERIFICATION_POINTS)));
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            RiskEvaluationResponse response = riskEngineService.evaluate(request);

            assertEquals(RiskAction.FULL_VERIFICATION_REQUIRED, response.action());
        }

        @Test
        void shouldResolveAuthorizationRequiredWhenNonLoginScoreReachesAuthorizationThreshold() {
            when(thresholds.getFullVerificationPoints()).thenReturn(FULL_VERIFICATION_POINTS);
            when(thresholds.getAuthorizationPoints()).thenReturn(AUTHORIZATION_POINTS);
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.EXPENSE_HIGH, AUTHORIZATION_POINTS)));
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            RiskEvaluationResponse response = riskEngineService.evaluate(request);

            assertEquals(RiskAction.AUTHORIZATION_REQUIRED, response.action());
        }

        @Test
        void shouldResolveSoftChallengeWhenNonLoginScoreReachesSoftChallengeThreshold() {
            when(thresholds.getFullVerificationPoints()).thenReturn(FULL_VERIFICATION_POINTS);
            when(thresholds.getAuthorizationPoints()).thenReturn(AUTHORIZATION_POINTS);
            when(thresholds.getSoftChallengePoints()).thenReturn(SOFT_CHALLENGE_POINTS);
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.EXPENSE_RECORD, SOFT_CHALLENGE_POINTS)));
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            RiskEvaluationResponse response = riskEngineService.evaluate(request);

            assertEquals(RiskAction.SOFT_CHALLENGE, response.action());
        }

        @Test
        void shouldResolveLogOnlyWhenNonLoginScoreIsBelowAllThresholds() {
            when(thresholds.getFullVerificationPoints()).thenReturn(FULL_VERIFICATION_POINTS);
            when(thresholds.getAuthorizationPoints()).thenReturn(AUTHORIZATION_POINTS);
            when(thresholds.getSoftChallengePoints()).thenReturn(SOFT_CHALLENGE_POINTS);
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.EXPENSE_NEW_CATEGORY, SOFT_CHALLENGE_POINTS - 1)));
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            RiskEvaluationResponse response = riskEngineService.evaluate(request);

            assertEquals(RiskAction.LOG_ONLY, response.action());
        }

        @Test
        void shouldResolveNonLoginActionWhenTriggerTypeIsNull() {
            when(thresholds.getFullVerificationPoints()).thenReturn(FULL_VERIFICATION_POINTS);
            when(thresholds.getAuthorizationPoints()).thenReturn(AUTHORIZATION_POINTS);
            when(thresholds.getSoftChallengePoints()).thenReturn(SOFT_CHALLENGE_POINTS);
            stubNoRiskRulesTriggered();
            RiskEvaluationRequest request = requestWithTriggerType(null);

            RiskEvaluationResponse response = riskEngineService.evaluate(request);

            assertEquals(RiskAction.LOG_ONLY, response.action());
        }
    }

    @Nested
    class EvaluateScoreClamping {

        @BeforeEach
        void setUp() {
            when(riskOperationRepository.findBySourceEventId(SOURCE_EVENT_ID)).thenReturn(Optional.empty());
            stubEmptyProfiles();
            when(thresholds.getFullVerificationPoints()).thenReturn(FULL_VERIFICATION_POINTS);
            when(thresholds.getAuthorizationPoints()).thenReturn(AUTHORIZATION_POINTS);
            when(thresholds.getSoftChallengePoints()).thenReturn(SOFT_CHALLENGE_POINTS);
        }

        @Test
        void shouldClampScoreToZeroWhenSumOfPointsIsNegative() {
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.REVENUE_LOW, -50)));
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            RiskEvaluationResponse response = riskEngineService.evaluate(request);

            assertEquals(0, response.score());
        }

        void shouldReturnZeroScoreWhenNoRulesAreTriggered() {
            stubNoRiskRulesTriggered();
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            RiskEvaluationResponse response = riskEngineService.evaluate(request);

            assertEquals(0, response.score());
            assertEquals(RiskAction.LOG_ONLY, response.action());
        }

        @Test
        void shouldSumPointsAcrossAllRiskServicesWhenMultipleRulesAreTriggered() {
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.EXPENSE_HIGH, 10)));
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.UNKNOWN_DEVICE, 15)));
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.REPEAT_CHANGE, 5)));
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            RiskEvaluationResponse response = riskEngineService.evaluate(request);

            assertEquals(30, response.score());
        }
    }

    @Nested
    class EvaluateEmailChallenge {

        @BeforeEach
        void setUp() {
            when(riskOperationRepository.findBySourceEventId(SOURCE_EVENT_ID)).thenReturn(Optional.empty());
            stubEmptyProfiles();
        }

        @Test
        void shouldNotSendEmailCodeWhenActionIsLogOnly() {
            when(thresholds.getLoginLogOnlyMaxPoints()).thenReturn(LOGIN_LOG_ONLY_MAX_POINTS);
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.MANY_KNOWN_DEVICES, LOGIN_LOG_ONLY_MAX_POINTS)));
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.LOGIN);

            riskEngineService.evaluate(request);

            verify(riskChallengeService, never()).generateAndSendEmailCode(any(RiskOperation.class), anyString());
        }

        @Test
        void shouldSendEmailCodeWhenActionIsAuthorizationRequired() {
            when(thresholds.getLoginLogOnlyMaxPoints()).thenReturn(LOGIN_LOG_ONLY_MAX_POINTS);
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.IMPOSSIBLE_TRAVEL, LOGIN_LOG_ONLY_MAX_POINTS + 1)));
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.LOGIN);

            riskEngineService.evaluate(request);

            verify(riskChallengeService, times(1)).generateAndSendEmailCode(any(RiskOperation.class), eq(EMAIL));
        }

        @Test
        void shouldSendEmailCodeWhenActionIsFullVerificationRequired() {
            when(thresholds.getFullVerificationPoints()).thenReturn(FULL_VERIFICATION_POINTS);
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.EXPENSE_HIGH, FULL_VERIFICATION_POINTS)));
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            riskEngineService.evaluate(request);

            verify(riskChallengeService, times(1)).generateAndSendEmailCode(any(RiskOperation.class), eq(EMAIL));
        }

        @Test
        void shouldNotSendEmailCodeWhenActionIsSoftChallenge() {
            when(thresholds.getFullVerificationPoints()).thenReturn(FULL_VERIFICATION_POINTS);
            when(thresholds.getAuthorizationPoints()).thenReturn(AUTHORIZATION_POINTS);
            when(thresholds.getSoftChallengePoints()).thenReturn(SOFT_CHALLENGE_POINTS);
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.EXPENSE_RECORD, SOFT_CHALLENGE_POINTS)));
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            riskEngineService.evaluate(request);

            verify(riskChallengeService, never()).generateAndSendEmailCode(any(RiskOperation.class), anyString());
        }
    }

    @Nested
    class EvaluateContextBuilding {

        @BeforeEach
        void setUp() {
            when(riskOperationRepository.findBySourceEventId(SOURCE_EVENT_ID)).thenReturn(Optional.empty());
            when(thresholds.getFullVerificationPoints()).thenReturn(FULL_VERIFICATION_POINTS);
            when(thresholds.getAuthorizationPoints()).thenReturn(AUTHORIZATION_POINTS);
            when(thresholds.getSoftChallengePoints()).thenReturn(SOFT_CHALLENGE_POINTS);
            stubNoRiskRulesTriggered();
        }

        @Test
        void shouldPassProfilesFromRepositoriesIntoContextWhenProfilesArePresent() {
            when(transactionProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(transactionProfile));
            when(loginProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(loginProfile));
            when(accountChangeProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(accountChangeProfile));
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            riskEngineService.evaluate(request);

            verify(transactionRiskService).evaluate(contextCaptor.capture());
            RiskContext context = contextCaptor.getValue();
            assertEquals(USER_ID, context.userId());
            assertEquals(RiskTriggerType.EXPENSE, context.triggerType());
            assertEquals(AMOUNT, context.amount());
            assertEquals(CATEGORY, context.category());
            assertEquals(IP_ADDRESS, context.ipAddress());
            assertEquals(LOCATION, context.location());
            assertEquals(BROWSER, context.browser());
            assertEquals(transactionProfile, context.transactionProfile());
            assertEquals(loginProfile, context.loginProfile());
            assertEquals(accountChangeProfile, context.accountChangeProfile());
        }

        @Test
        void shouldPassNullProfilesIntoContextWhenProfilesAreAbsent() {
            stubEmptyProfiles();
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            riskEngineService.evaluate(request);

            verify(loginRiskService).evaluate(contextCaptor.capture());
            RiskContext context = contextCaptor.getValue();
            assertNull(context.transactionProfile());
            assertNull(context.loginProfile());
            assertNull(context.accountChangeProfile());
        }

        @Test
        void shouldUseSameContextInstanceAcrossAllRiskServices() {
            stubEmptyProfiles();
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            riskEngineService.evaluate(request);

            verify(transactionRiskService).evaluate(contextCaptor.capture());
            RiskContext capturedContext = contextCaptor.getValue();
            verify(loginRiskService).evaluate(eq(capturedContext));
            verify(accountChangeRiskService).evaluate(eq(capturedContext));
        }
    }

    @Nested
    class EvaluatePersistence {

        @BeforeEach
        void setUp() {
            when(riskOperationRepository.findBySourceEventId(SOURCE_EVENT_ID)).thenReturn(Optional.empty());
            stubEmptyProfiles();
            when(thresholds.getFullVerificationPoints()).thenReturn(FULL_VERIFICATION_POINTS);
            when(thresholds.getAuthorizationPoints()).thenReturn(AUTHORIZATION_POINTS);
            when(thresholds.getSoftChallengePoints()).thenReturn(SOFT_CHALLENGE_POINTS);
        }

        @Test
        void shouldPersistOperationWithFieldsFromRequestAndComputedScoreWhenNewSourceEventId() {
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.EXPENSE_HIGH, 25)));
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            riskEngineService.evaluate(request);

            verify(riskOperationRepository).save(operationCaptor.capture());
            RiskOperation savedOperation = operationCaptor.getValue();
            assertEquals(SOURCE_EVENT_ID, savedOperation.getSourceEventId());
            assertEquals(RiskTriggerType.EXPENSE, savedOperation.getTriggerType());
            assertEquals(USER_ID, savedOperation.getUserId());
            assertEquals(25, savedOperation.getScore());
            assertEquals(RiskAction.LOG_ONLY, savedOperation.getAction());
            assertNotNull(savedOperation.getOperationDate());
            assertNotNull(savedOperation.getCreatedAt());
        }

        @Test
        void shouldBuildRuleCollectionsMatchingTriggeredRulesWhenRulesAreTriggered() {
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.EXPENSE_HIGH, 10)));
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.UNKNOWN_DEVICE, 15)));
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            riskEngineService.evaluate(request);

            verify(riskOperationRepository).save(operationCaptor.capture());
            RiskOperation savedOperation = operationCaptor.getValue();
            List<RiskRuleCollection> collections = savedOperation.getRiskRuleCollections();
            assertEquals(2, collections.size());
            assertEquals(RiskRule.EXPENSE_HIGH, collections.get(0).getRiskRule());
            assertEquals(10, collections.get(0).getScorePerRule());
            assertEquals(savedOperation, collections.get(0).getRiskOperation());
            assertEquals(RiskRule.UNKNOWN_DEVICE, collections.get(1).getRiskRule());
            assertEquals(15, collections.get(1).getScorePerRule());
            assertEquals(savedOperation, collections.get(1).getRiskOperation());
        }

        @Test
        void shouldBuildEmptyRuleCollectionsWhenNoRulesAreTriggered() {
            stubNoRiskRulesTriggered();
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            riskEngineService.evaluate(request);

            verify(riskOperationRepository).save(operationCaptor.capture());
            assertTrue(operationCaptor.getValue().getRiskRuleCollections().isEmpty());
        }

        @Test
        void shouldReturnResponseBuiltFromPersistedOperationWhenNewSourceEventId() {
            stubNoRiskRulesTriggered();
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            RiskEvaluationResponse response = riskEngineService.evaluate(request);

            assertEquals(0, response.score());
            assertEquals(RiskAction.LOG_ONLY, response.action());
            assertFalse(response.passwordRequired());
            assertFalse(response.emailCodeRequired());
            assertTrue(response.isFullyVerified());
        }
    }

    @Nested
    class EvaluateExceptionPropagation {

        @BeforeEach
        void setUp() {
            when(riskOperationRepository.findBySourceEventId(SOURCE_EVENT_ID)).thenReturn(Optional.empty());
            stubEmptyProfiles();
        }

        @Test
        void shouldThrowExceptionWhenTransactionRiskServiceThrowsException() {
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenThrow(new IllegalStateException("transaction risk failure"));
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            assertThrows(IllegalStateException.class, () -> riskEngineService.evaluate(request));
        }

        @Test
        void shouldThrowExceptionWhenRiskOperationRepositorySaveThrowsException() {
            when(thresholds.getFullVerificationPoints()).thenReturn(FULL_VERIFICATION_POINTS);
            when(thresholds.getAuthorizationPoints()).thenReturn(AUTHORIZATION_POINTS);
            when(thresholds.getSoftChallengePoints()).thenReturn(SOFT_CHALLENGE_POINTS);
            stubNoRiskRulesTriggered();
            doThrow(new RuntimeException("persistence failure")).when(riskOperationRepository).save(any(RiskOperation.class));
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            assertThrows(RuntimeException.class, () -> riskEngineService.evaluate(request));
        }

        @Test
        void shouldThrowExceptionWhenRiskChallengeServiceThrowsException() {
            when(thresholds.getFullVerificationPoints()).thenReturn(FULL_VERIFICATION_POINTS);
            when(transactionRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of(new TriggeredRule(RiskRule.EXPENSE_HIGH, FULL_VERIFICATION_POINTS)));
            when(loginRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            when(accountChangeRiskService.evaluate(any(RiskContext.class))).thenReturn(List.of());
            doThrow(new RuntimeException("email challenge failure")).when(riskChallengeService).generateAndSendEmailCode(any(RiskOperation.class), anyString());
            RiskEvaluationRequest request = requestWithTriggerType(RiskTriggerType.EXPENSE);

            assertThrows(RuntimeException.class, () -> riskEngineService.evaluate(request));
        }
    }

    @Nested
    class EvaluateInputValidation {

        @Test
        void shouldThrowNullPointerExceptionWhenRequestIsNull() {
            assertThrows(NullPointerException.class, () -> riskEngineService.evaluate(null));
        }
    }
}