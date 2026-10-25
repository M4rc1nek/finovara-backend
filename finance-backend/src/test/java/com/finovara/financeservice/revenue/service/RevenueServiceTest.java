package com.finovara.financeservice.revenue.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.mainaccount.activity.event.revenue.RevenueActivityEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.mapper.RevenueMapper;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.riskverification.service.RiskGuardService;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import com.finovara.financeservice.util.transaction.revenue.RevenueManagerService;
import com.finovara.financeservice.wallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long REVENUE_ID = 5L;
    private static final String AUTH_CODE = "123456";
    private static final String REVENUE_AGGREGATE = "Revenue";
    private static final String REVENUE_TOPIC = "revenue.created";
    private static final int PAGE_SIZE = 10;
    private static final BigDecimal AMOUNT = BigDecimal.TEN;
    private static final RevenueCategory CATEGORY = RevenueCategory.values()[0];
    private static final TransactionOrigin AUTOMATIC_ORIGIN = Arrays.stream(TransactionOrigin.values())
            .filter(origin -> origin != TransactionOrigin.USER_MANUAL)
            .findFirst()
            .orElseThrow();

    @Mock
    private OutboxService outboxService;

    @Mock
    private RevenueRepository revenueRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private RevenueManagerService revenueManagerService;

    @Mock
    private RevenueMapper revenueMapper;

    @Mock
    private AutoPaymentsService autoPaymentsService;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Mock
    private RiskGuardService riskGuardService;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private RevenueDto revenueDto;

    @Captor
    private ArgumentCaptor<Revenue> revenueCaptor;

    @Captor
    private ArgumentCaptor<List<Revenue>> revenuesCaptor;

    @InjectMocks
    private RevenueService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", PAGE_SIZE);
    }

    @Nested
    class AddRevenue {

        @Nested
        class AmountValidation {

            @Test
            void shouldThrowExceptionWhenAmountIsNull() {
                assertThrows(InvalidInputException.class, () -> service.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                verifyNoInteractions(riskGuardService, walletService, revenueRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenAmountIsZero() {
                when(revenueDto.amount()).thenReturn(BigDecimal.ZERO);

                assertThrows(InvalidInputException.class, () -> service.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                verifyNoInteractions(riskGuardService, walletService, revenueRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenAmountIsNegative() {
                when(revenueDto.amount()).thenReturn(BigDecimal.valueOf(-1));

                assertThrows(InvalidInputException.class, () -> service.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                verifyNoInteractions(riskGuardService, walletService, revenueRepository, outboxService);
            }
        }

        @Nested
        class ValidAmount {

            private Revenue savedRevenue;

            @BeforeEach
            void setUp() {
                when(revenueDto.amount()).thenReturn(AMOUNT);
                when(revenueDto.category()).thenReturn(CATEGORY);
                savedRevenue = Revenue.builder().id(REVENUE_ID).amount(AMOUNT).category(CATEGORY).userId(USER_ID).build();
            }

            @Test
            void shouldSaveRevenueAndReturnIdWhenOriginIsUserManual() {
                when(revenueRepository.save(any(Revenue.class))).thenReturn(savedRevenue);

                Long result = service.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                assertEquals(REVENUE_ID, result);
                verify(revenueRepository).save(revenueCaptor.capture());
                assertEquals(AMOUNT, revenueCaptor.getValue().getAmount());
                assertEquals(CATEGORY, revenueCaptor.getValue().getCategory());
                assertEquals(USER_ID, revenueCaptor.getValue().getUserId());
                assertEquals(LocalDate.now(), revenueCaptor.getValue().getCreatedAt());
            }

            @Test
            void shouldGuardRiskAndConfirmAuthorizationWhenOriginIsUserManual() {
                when(revenueRepository.save(any(Revenue.class))).thenReturn(savedRevenue);

                service.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                InOrder inOrder = inOrder(riskGuardService, authBackendClient);
                inOrder.verify(riskGuardService).guard(eq(USER_ID), eq(RiskTriggerType.REVENUE), eq(AMOUNT), eq(CATEGORY.name()), any(), any(), eq(servletRequest));
                inOrder.verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            }

            @Test
            void shouldSkipRiskGuardAndAuthorizationWhenOriginIsNotUserManual() {
                when(revenueRepository.save(any(Revenue.class))).thenReturn(savedRevenue);

                Long result = service.addRevenue(revenueDto, USER_ID, servletRequest, AUTOMATIC_ORIGIN);

                assertEquals(REVENUE_ID, result);
                verifyNoInteractions(riskGuardService, authBackendClient, additionalAuthorizationCodeResolver);
            }

            @Test
            void shouldApplyAllSideEffectsWhenRevenueAdded() {
                when(revenueRepository.save(any(Revenue.class))).thenReturn(savedRevenue);

                service.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL);

                verify(walletService).addBalanceToWallet(USER_ID, AMOUNT);
                verify(outboxService).save(eq(REVENUE_AGGREGATE), eq(REVENUE_ID.toString()), eq(REVENUE_TOPIC), any(RevenueActivityEvent.class));
                verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, PiggyBankAutomationMode.APPLY);
            }

            @Test
            void shouldThrowExceptionWhenRiskGuardRejectsRevenue() {
                doThrow(new IllegalStateException()).when(riskGuardService)
                        .guard(any(), any(), any(), any(), any(), any(), any());

                assertThrows(IllegalStateException.class, () -> service.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                verify(authBackendClient, never()).confirmAuthorizationCode(any(), any());
                verifyNoInteractions(walletService, revenueRepository, outboxService, autoPaymentsService);
            }

            @Test
            void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
                doThrow(new IllegalStateException()).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

                assertThrows(IllegalStateException.class, () -> service.addRevenue(revenueDto, USER_ID, servletRequest, TransactionOrigin.USER_MANUAL));
                verifyNoInteractions(walletService, revenueRepository, outboxService, autoPaymentsService);
            }
        }

        @Nested
        class MinimalAmount {

            @Test
            void shouldSaveRevenueWhenAmountIsSmallestPositiveValue() {
                BigDecimal smallestAmount = new BigDecimal("0.01");
                Revenue savedRevenue = Revenue.builder().id(REVENUE_ID).amount(smallestAmount).category(CATEGORY).userId(USER_ID).build();
                when(revenueDto.amount()).thenReturn(smallestAmount);
                when(revenueDto.category()).thenReturn(CATEGORY);
                when(revenueRepository.save(any(Revenue.class))).thenReturn(savedRevenue);

                Long result = service.addRevenue(revenueDto, USER_ID, servletRequest, AUTOMATIC_ORIGIN);

                assertEquals(REVENUE_ID, result);
                verify(walletService).addBalanceToWallet(USER_ID, smallestAmount);
            }
        }
    }

    @Nested
    class EditRevenue {

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThrows(InvalidInputException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));
            verifyNoInteractions(authBackendClient, riskGuardService, walletService, revenueManagerService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsZero() {
            when(revenueDto.amount()).thenReturn(BigDecimal.ZERO);

            assertThrows(InvalidInputException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));
            verifyNoInteractions(authBackendClient, riskGuardService, walletService, revenueManagerService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNegative() {
            when(revenueDto.amount()).thenReturn(BigDecimal.valueOf(-10));

            assertThrows(InvalidInputException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));
            verifyNoInteractions(authBackendClient, riskGuardService, walletService, revenueManagerService);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            when(revenueDto.amount()).thenReturn(AMOUNT);
            doThrow(new IllegalStateException()).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));
            verifyNoInteractions(riskGuardService, walletService, revenueManagerService, autoPaymentsService, outboxService);
        }

        @Nested
        class Authorized {

            private Revenue existing;

            @BeforeEach
            void setUp() {
                when(revenueDto.amount()).thenReturn(AMOUNT);
                when(revenueDto.category()).thenReturn(CATEGORY);
                existing = Revenue.builder().id(REVENUE_ID).amount(BigDecimal.valueOf(5)).category(CATEGORY).userId(USER_ID).build();
            }

            @Test
            void shouldUpdateRevenueAndAddBalanceWhenAmountIncreased() {
                when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID)).thenReturn(existing);

                Long result = service.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest);

                assertEquals(REVENUE_ID, result);
                assertEquals(AMOUNT, existing.getAmount());
                assertEquals(CATEGORY, existing.getCategory());
                verify(walletService).addBalanceToWallet(USER_ID, BigDecimal.valueOf(5));
                verify(walletService, never()).removeBalanceFromWallet(any(), any());
            }

            @Test
            void shouldRemoveBalanceWhenAmountDecreased() {
                existing.setAmount(BigDecimal.valueOf(25));
                when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID)).thenReturn(existing);

                service.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest);

                verify(walletService).removeBalanceFromWallet(USER_ID, BigDecimal.valueOf(15));
                verify(walletService, never()).addBalanceToWallet(any(), any());
            }

            @Test
            void shouldNotChangeWalletBalanceWhenAmountUnchanged() {
                existing.setAmount(AMOUNT);
                when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID)).thenReturn(existing);

                service.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest);

                verify(walletService, never()).addBalanceToWallet(any(), any());
                verify(walletService, never()).removeBalanceFromWallet(any(), any());
            }

            @Test
            void shouldGuardRiskAndLockWalletWhenRevenueEdited() {
                when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID)).thenReturn(existing);

                service.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest);

                verify(riskGuardService).guard(eq(USER_ID), eq(RiskTriggerType.REVENUE), eq(AMOUNT), eq(CATEGORY.name()), any(), any(), eq(servletRequest));
                verify(walletService).lockWalletForUpdate(USER_ID);
            }

            @Test
            void shouldRollbackOldAmountBeforeApplyingNewAmountToPiggyBank() {
                when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID)).thenReturn(existing);

                service.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest);

                InOrder inOrder = inOrder(autoPaymentsService);
                inOrder.verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, BigDecimal.valueOf(5), PiggyBankAutomationMode.ROLLBACK);
                inOrder.verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, PiggyBankAutomationMode.APPLY);
            }

            @Test
            void shouldSaveOutboxEventWhenRevenueEdited() {
                when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID)).thenReturn(existing);

                service.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest);

                verify(outboxService).save(eq(REVENUE_AGGREGATE), eq(REVENUE_ID.toString()), eq(REVENUE_TOPIC), any(RevenueActivityEvent.class));
            }

            @Test
            void shouldThrowExceptionWhenRevenueDoesNotExist() {
                when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID)).thenThrow(new IllegalStateException());

                assertThrows(IllegalStateException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));
                verify(walletService, never()).addBalanceToWallet(any(), any());
                verify(walletService, never()).removeBalanceFromWallet(any(), any());
                verifyNoInteractions(autoPaymentsService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenRiskGuardRejectsRevenue() {
                doThrow(new IllegalStateException()).when(riskGuardService)
                        .guard(any(), any(), any(), any(), any(), any(), any());

                assertThrows(IllegalStateException.class, () -> service.editRevenue(revenueDto, REVENUE_ID, USER_ID, servletRequest));
                verifyNoInteractions(walletService, revenueManagerService, autoPaymentsService, outboxService);
            }
        }
    }

    @Nested
    class DeleteRevenue {

        private Revenue revenue;

        @BeforeEach
        void setUp() {
            revenue = Revenue.builder().id(REVENUE_ID).amount(AMOUNT).category(CATEGORY).userId(USER_ID).build();
        }

        @Test
        void shouldDeleteRevenueAndRemoveBalanceWhenRevenueExists() {
            when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID)).thenReturn(revenue);

            service.deleteRevenue(REVENUE_ID, USER_ID, AUTH_CODE);

            verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
            verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            verify(walletService).lockWalletForUpdate(USER_ID);
            verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, PiggyBankAutomationMode.ROLLBACK);
            verify(walletService).removeBalanceFromWallet(USER_ID, AMOUNT);
            verify(outboxService).save(eq(REVENUE_AGGREGATE), eq(REVENUE_ID.toString()), eq(REVENUE_TOPIC), any(RevenueActivityEvent.class));
            verify(revenueRepository).delete(revenue);
        }

        @Test
        void shouldRollbackPiggyBankBeforeRemovingBalance() {
            when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID)).thenReturn(revenue);

            service.deleteRevenue(REVENUE_ID, USER_ID, AUTH_CODE);

            InOrder inOrder = inOrder(autoPaymentsService, walletService, revenueRepository);
            inOrder.verify(autoPaymentsService).handleRevenuePiggyBankAutomation(USER_ID, AMOUNT, PiggyBankAutomationMode.ROLLBACK);
            inOrder.verify(walletService).removeBalanceFromWallet(USER_ID, AMOUNT);
            inOrder.verify(revenueRepository).delete(revenue);
        }

        @Test
        void shouldThrowExceptionWhenRevenueDoesNotExist() {
            when(revenueManagerService.getRevenueByUserIdOrThrow(REVENUE_ID, USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.deleteRevenue(REVENUE_ID, USER_ID, AUTH_CODE));
            verify(walletService, never()).removeBalanceFromWallet(any(), any());
            verifyNoInteractions(autoPaymentsService, outboxService, revenueRepository);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            doThrow(new IllegalStateException()).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.deleteRevenue(REVENUE_ID, USER_ID, AUTH_CODE));
            verifyNoInteractions(walletService, revenueManagerService, autoPaymentsService, outboxService, revenueRepository);
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteAllRevenuesWhenUserHasRevenues() {
            Revenue revenue = Revenue.builder().id(REVENUE_ID).amount(AMOUNT).category(CATEGORY).userId(USER_ID).build();
            when(revenueRepository.findAllByUserId(USER_ID)).thenReturn(List.of(revenue));

            service.deleteByUserId(USER_ID);

            verify(revenueRepository).deleteAll(revenuesCaptor.capture());
            assertEquals(List.of(revenue), revenuesCaptor.getValue());
        }

        @Test
        void shouldDeleteEmptyListWhenUserHasNoRevenues() {
            when(revenueRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

            service.deleteByUserId(USER_ID);

            verify(revenueRepository).deleteAll(revenuesCaptor.capture());
            assertTrue(revenuesCaptor.getValue().isEmpty());
        }
    }
}