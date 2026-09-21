package com.finovara.financeservice.revenue.service;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.model.activity.RevenueActivityType;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.securitymonitoring.dto.RiskTriggerType;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.mapper.RevenueMapper;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.riskverification.service.RiskGuardService;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import com.finovara.financeservice.util.transaction.revenue.RevenueManagerService;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.wallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {

    @Mock
    private RevenueRepository revenueRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private AutoPaymentsService autoPaymentsService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private RevenueManagerService revenueManagerService;
    @Mock
    private RevenueMapper revenueMapper;
    @Mock
    private AuthBackendClient authBackendClient;
    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;
    @Mock
    private RiskGuardService riskGuardService;
    @Mock
    private HttpServletRequest servletRequest;

    @InjectMocks
    private RevenueService revenueService;

    private Long userId;

    private static final String USER_EMAIL = "user@test.com";
    private static final String RISK_SOURCE_EVENT_ID = "risk-source-event-id";

    @BeforeEach
    void setUp() {
        userId = 1L;
    }

    @Nested
    class AddRevenueTests {

        @Test
        void shouldAddRevenueSuccessfully() {
            RevenueDto dto = new RevenueDto(2L, userId, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edit", null, RISK_SOURCE_EVENT_ID);

            when(revenueRepository.save(any(Revenue.class)))
                    .thenAnswer(invocation -> {
                        Revenue r = invocation.getArgument(0);
                        r.setId(1L);
                        return r;
                    });

            revenueService.addRevenue(dto, userId, servletRequest, TransactionOrigin.USER_MANUAL);

            verify(walletService).addBalanceToWallet(userId, dto.amount());
            verify(revenueRepository).save(any(Revenue.class));

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxService).save(eq("Revenue"), any(), eq("revenue.created"), payloadCaptor.capture());

            RevenueActivityEvent event = (RevenueActivityEvent) payloadCaptor.getValue();
            assertEquals(RevenueActivityType.ADDED_REVENUE, event.type());

            verify(autoPaymentsService).handleRevenuePiggyBankAutomation(userId, dto.amount(), PiggyBankAutomationMode.APPLY);
        }

        @Test
        void shouldConfirmAuthorizationCodeWhenOriginIsUserManual() {
            RevenueDto dto = new RevenueDto(2L, userId, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edit", "111111", RISK_SOURCE_EVENT_ID);

            when(revenueRepository.save(any(Revenue.class)))
                    .thenAnswer(invocation -> {
                        Revenue r = invocation.getArgument(0);
                        r.setId(1L);
                        return r;
                    });
            when(additionalAuthorizationCodeResolver.resolve("111111")).thenReturn(new ConfirmAuthorizationCodeDto("111111"));

            revenueService.addRevenue(dto, userId, servletRequest, TransactionOrigin.USER_MANUAL);

            verify(authBackendClient).confirmAuthorizationCode(eq(userId), any(ConfirmAuthorizationCodeDto.class));
        }

        @Test
        void shouldGuardAgainstRiskWhenOriginIsUserManual() {
            RevenueDto dto = new RevenueDto(2L, userId, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edit", null, RISK_SOURCE_EVENT_ID);

            when(revenueRepository.save(any(Revenue.class)))
                    .thenAnswer(invocation -> {
                        Revenue r = invocation.getArgument(0);
                        r.setId(1L);
                        return r;
                    });
            when(authBackendClient.getUserEmail(userId)).thenReturn(USER_EMAIL);

            revenueService.addRevenue(dto, userId, servletRequest, TransactionOrigin.USER_MANUAL);

            verify(riskGuardService).guard(userId, RiskTriggerType.REVENUE, dto.amount(), RevenueCategory.INVESTMENT.name(), USER_EMAIL, RISK_SOURCE_EVENT_ID, servletRequest);
        }

        @Test
        void shouldNotAddRevenueWhenRiskGuardFails() {
            RevenueDto dto = new RevenueDto(2L, userId, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edit", null, RISK_SOURCE_EVENT_ID);
            doThrow(new RuntimeException("Risk detected"))
                    .when(riskGuardService).guard(eq(userId), eq(RiskTriggerType.REVENUE), any(), any(), any(), any(), eq(servletRequest));

            assertThrows(RuntimeException.class, () -> revenueService.addRevenue(dto, userId, servletRequest, TransactionOrigin.USER_MANUAL));

            verifyNoInteractions(walletService);
            verify(revenueRepository, never()).save(any());
        }

        @Test
        void shouldSkipAuthorizationCodeConfirmationWhenOriginIsRecurringSystem() {
            RevenueDto dto = new RevenueDto(null, userId, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "recurring", null, RISK_SOURCE_EVENT_ID);

            when(revenueRepository.save(any(Revenue.class)))
                    .thenAnswer(invocation -> {
                        Revenue r = invocation.getArgument(0);
                        r.setId(1L);
                        return r;
                    });

            revenueService.addRevenue(dto, userId, servletRequest, TransactionOrigin.RECURRING_SYSTEM);

            verifyNoInteractions(authBackendClient, additionalAuthorizationCodeResolver, riskGuardService);
        }
    }

    @Nested
    class EditRevenueTests {

        @Test
        void shouldEditRevenueSuccessfully() {
            Revenue revenue = new Revenue();
            revenue.setId(10L);
            revenue.setUserId(userId);
            revenue.setAmount(new BigDecimal("50"));
            revenue.setCategory(RevenueCategory.SALARY);

            RevenueDto dto = new RevenueDto(null, null, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edit", null, RISK_SOURCE_EVENT_ID);

            when(revenueManagerService.getRevenueOrThrow(10L)).thenReturn(revenue);

            revenueService.editRevenue(dto, 10L, userId, servletRequest);

            verify(autoPaymentsService).handleRevenuePiggyBankAutomation(userId, new BigDecimal("50"), PiggyBankAutomationMode.ROLLBACK);
            verify(autoPaymentsService).handleRevenuePiggyBankAutomation(userId, new BigDecimal("100"), PiggyBankAutomationMode.APPLY);

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxService).save(
                    eq("Revenue"),
                    eq("10"),
                    eq("revenue.created"),
                    payloadCaptor.capture()
            );
            RevenueActivityEvent event = (RevenueActivityEvent) payloadCaptor.getValue();
            assertEquals(RevenueActivityType.EDITED_REVENUE, event.type());

            verify(revenueRepository).save(revenue);
        }

        @Test
        void shouldGuardAgainstRiskWhenEditingRevenue() {
            Revenue revenue = new Revenue();
            revenue.setId(10L);
            revenue.setUserId(userId);
            revenue.setAmount(new BigDecimal("50"));
            revenue.setCategory(RevenueCategory.SALARY);

            RevenueDto dto = new RevenueDto(null, null, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edit", null, RISK_SOURCE_EVENT_ID);

            when(revenueManagerService.getRevenueOrThrow(10L)).thenReturn(revenue);
            when(authBackendClient.getUserEmail(userId)).thenReturn(USER_EMAIL);

            revenueService.editRevenue(dto, 10L, userId, servletRequest);

            verify(riskGuardService).guard(userId, RiskTriggerType.REVENUE, dto.amount(), RevenueCategory.INVESTMENT.name(), USER_EMAIL, RISK_SOURCE_EVENT_ID, servletRequest);
        }

        @Test
        void shouldNotEditRevenueWhenRiskGuardFails() {
            Revenue revenue = new Revenue();
            revenue.setId(10L);
            revenue.setUserId(userId);
            revenue.setAmount(new BigDecimal("50"));
            revenue.setCategory(RevenueCategory.SALARY);

            RevenueDto dto = new RevenueDto(null, null, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edit", null, RISK_SOURCE_EVENT_ID);

            when(revenueManagerService.getRevenueOrThrow(10L)).thenReturn(revenue);
            doThrow(new RuntimeException("Risk detected"))
                    .when(riskGuardService).guard(eq(userId), eq(RiskTriggerType.REVENUE), any(), any(), any(), any(), eq(servletRequest));

            assertThrows(RuntimeException.class, () -> revenueService.editRevenue(dto, 10L, userId, servletRequest));

            verify(revenueRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenRevenueBelongsToOtherUser() {
            Revenue revenue = new Revenue();
            revenue.setId(10L);
            revenue.setUserId(2L);

            when(revenueManagerService.getRevenueOrThrow(10L)).thenReturn(revenue);

            RevenueDto dto = new RevenueDto(null, null, BigDecimal.TEN, RevenueCategory.SALARY, null, "x", null, RISK_SOURCE_EVENT_ID);

            assertThrows(RequestedEntityNotFoundException.class, () ->
                    revenueService.editRevenue(dto, 10L, userId, servletRequest));

            verify(revenueRepository, never()).save(any());
            verify(outboxService, never()).save(any(), any(), any(), any());
            verifyNoInteractions(riskGuardService);
        }
    }

    @Nested
    class GetRevenueTests {

        @Test
        void shouldReturnRevenueList() {
            Revenue revenue1 = new Revenue();
            Revenue revenue2 = new Revenue();
            when(revenueRepository.findAllByUserId(userId)).thenReturn(List.of(revenue1, revenue2));
            when(revenueMapper.mapRevenueToDto(any())).thenReturn(new RevenueDto(null, null, BigDecimal.TEN, RevenueCategory.SALARY, null, "x", null, RISK_SOURCE_EVENT_ID));

            List<RevenueDto> result = revenueService.getRevenue(userId);

            assertEquals(2, result.size());
            verify(revenueMapper, times(2)).mapRevenueToDto(any());
        }

        @Test
        void shouldReturnEmptyList() {
            when(revenueRepository.findAllByUserId(userId)).thenReturn(List.of());

            List<RevenueDto> result = revenueService.getRevenue(userId);

            assertTrue(result.isEmpty());
            verifyNoInteractions(revenueMapper);
        }
    }

    @Nested
    class DeleteRevenueTests {

        @Test
        void shouldDeleteRevenueSuccessfully() {
            Revenue revenue = new Revenue();
            revenue.setId(1L);
            revenue.setUserId(userId);
            revenue.setAmount(new BigDecimal("100"));
            revenue.setCategory(RevenueCategory.SALARY);

            when(revenueRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(revenue));

            revenueService.deleteRevenue(1L, userId, null);

            verify(autoPaymentsService).handleRevenuePiggyBankAutomation(userId, new BigDecimal("100"), PiggyBankAutomationMode.ROLLBACK);
            verify(walletService).removeBalanceFromWallet(userId, new BigDecimal("100"));

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(outboxService).save(
                    eq("Revenue"),
                    eq("1"),
                    eq("revenue.created"),
                    payloadCaptor.capture()
            );
            RevenueActivityEvent event = (RevenueActivityEvent) payloadCaptor.getValue();
            assertEquals(RevenueActivityType.DELETED_REVENUE, event.type());

            verify(revenueRepository).delete(revenue);
        }

        @Test
        void shouldThrowWhenRevenueNotFound() {
            when(revenueRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> revenueService.deleteRevenue(1L, userId, null));

            verify(revenueRepository, never()).delete(any());
            verify(outboxService, never()).save(any(), any(), any(), any());
        }
    }
}