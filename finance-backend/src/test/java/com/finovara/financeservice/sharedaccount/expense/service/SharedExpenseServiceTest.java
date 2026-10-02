package com.finovara.financeservice.sharedaccount.expense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.contracts.sharedaccount.event.activity.finance.SharedAccountExpenseActivityEvent;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseDto;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseRequest;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseResponse;
import com.finovara.financeservice.sharedaccount.expense.mapper.SharedExpenseMapper;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.dto.ExpenseAnalysisMode;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.service.ExpenseAnalysisService;
import com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.service.LargeExpenseNotificationService;
import com.finovara.financeservice.sharedaccount.settings.expense.spendcontrol.service.SpendControlService;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import com.finovara.financeservice.util.transaction.expense.SharedExpenseManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedExpenseServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long EXPENSE_ID = 100L;
    private static final Long OWNER_ID = 10L;
    private static final Long MEMBER_ID = 20L;
    private static final String USERNAME = "john";
    private static final String OTHER_USERNAME = "anna";
    private static final String DESCRIPTION = "Groceries";
    private static final BigDecimal AMOUNT = new BigDecimal("10.00");
    private static final BigDecimal OLD_AMOUNT = new BigDecimal("8.00");
    private static final BigDecimal LIMIT_AMOUNT = new BigDecimal("15.00");

    @Mock
    private SharedExpenseRepository sharedExpenseRepository;

    @Mock
    private SharedWalletService sharedWalletService;

    @Mock
    private SharedLimitRepository sharedLimitRepository;

    @Mock
    private FinancialPeriodService financialPeriodService;

    @Mock
    private SharedExpenseManagerService sharedExpenseManagerService;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private SpendControlService spendControlService;

    @Mock
    private ExpenseAnalysisService expenseAnalysisService;

    @Mock
    private LargeExpenseNotificationService largeExpenseNotificationService;

    @Mock
    private SharedExpenseMapper sharedExpenseMapper;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SharedExpenseRequest sharedExpenseRequest;

    @Mock
    private SharedExpenseDto sharedExpenseDto;

    @Mock
    private SharedExpenseDto firstMappedDto;

    @Mock
    private SharedExpenseDto secondMappedDto;

    @Mock
    private SharedAccountParticipantsResponse participants;

    @Mock
    private SharedExpense existingExpense;

    @Mock
    private SharedExpense savedExpense;

    @Mock
    private SharedExpense firstExpense;

    @Mock
    private SharedExpense secondExpense;

    @Mock
    private SharedLimit limit;

    @Mock
    private SharedLimit secondLimit;

    @Captor
    private ArgumentCaptor<SharedExpense> expenseCaptor;

    @InjectMocks
    private SharedExpenseService sharedExpenseService;

    private ExpenseCategory category;
    private ExpenseCategory otherCategory;

    @BeforeEach
    void setUp() {
        category = ExpenseCategory.values()[0];
        otherCategory = ExpenseCategory.values()[1];
    }

    @Nested
    class AddExpense {

        @BeforeEach
        void setUp() {
            when(sharedExpenseRequest.sharedExpenseDto()).thenReturn(sharedExpenseDto);
            when(sharedExpenseDto.category()).thenReturn(category);
            when(sharedExpenseDto.amount()).thenReturn(AMOUNT);
            when(sharedExpenseDto.description()).thenReturn(DESCRIPTION);
        }

        @Nested
        class Validation {

            @ParameterizedTest
            @ValueSource(strings = {"0.99", "0", "-5.00"})
            void shouldThrowExceptionWhenAmountIsLowerThanOne(BigDecimal amount) {
                when(sharedExpenseDto.amount()).thenReturn(amount);

                assertThrows(InvalidInputException.class, () -> sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID));
            }

            @Test
            void shouldNotInteractWithDependenciesWhenAmountIsLowerThanOne() {
                when(sharedExpenseDto.amount()).thenReturn(new BigDecimal("0.99"));

                assertThrows(InvalidInputException.class, () -> sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID));

                verifyNoInteractions(spendControlService, sharedLimitRepository, expenseAnalysisService, sharedAccountParticipantsService, authBackendClient, sharedWalletService, sharedExpenseRepository, outboxService, largeExpenseNotificationService);
            }

            @Test
            void shouldThrowExceptionWhenSpendControlFails() {
                doThrow(new IllegalStateException()).when(spendControlService).handleSpendControl(USER_ID, AMOUNT);

                assertThrows(IllegalStateException.class, () -> sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID));
                verifyNoInteractions(sharedLimitRepository, expenseAnalysisService, sharedWalletService, sharedExpenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenGeneralLimitIsExceeded() {
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit));
                when(financialPeriodService.getSharedExpensesSum(eq(USER_ID), any(), any())).thenReturn(new BigDecimal("6.00"));
                when(limit.getAmount()).thenReturn(LIMIT_AMOUNT);

                MissingRequirementException exception = assertThrows(MissingRequirementException.class, () -> sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID));

                assertEquals("General limit exceeded", exception.getMessage());
            }

            @Test
            void shouldThrowExceptionWhenCategoryLimitIsExceeded() {
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit));
                when(limit.getCategory()).thenReturn(category);
                when(financialPeriodService.getSharedExpensesSum(eq(USER_ID), any(), any())).thenReturn(new BigDecimal("6.00"));
                when(limit.getAmount()).thenReturn(LIMIT_AMOUNT);

                MissingRequirementException exception = assertThrows(MissingRequirementException.class, () -> sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID));

                assertEquals("Category limit exceeded", exception.getMessage());
            }

            @Test
            void shouldThrowExceptionWhenSecondLimitIsExceededAndFirstDoesNotApply() {
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit, secondLimit));
                when(limit.getCategory()).thenReturn(otherCategory);
                when(financialPeriodService.getSharedExpensesSum(eq(USER_ID), any(), any())).thenReturn(new BigDecimal("6.00"));
                when(secondLimit.getAmount()).thenReturn(LIMIT_AMOUNT);

                assertThrows(MissingRequirementException.class, () -> sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID));
            }

            @Test
            void shouldNotSaveExpenseWhenLimitIsExceeded() {
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit));
                when(financialPeriodService.getSharedExpensesSum(eq(USER_ID), any(), any())).thenReturn(new BigDecimal("6.00"));
                when(limit.getAmount()).thenReturn(LIMIT_AMOUNT);

                assertThrows(MissingRequirementException.class, () -> sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID));

                verifyNoInteractions(expenseAnalysisService, sharedWalletService, sharedExpenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenExpenseAnalysisFails() {
                doThrow(new IllegalStateException()).when(expenseAnalysisService).handleExpenseAnalysis(eq(USER_ID), any(), eq(AMOUNT), eq(ExpenseAnalysisMode.ADD));

                assertThrows(IllegalStateException.class, () -> sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID));
                verifyNoInteractions(sharedAccountParticipantsService, authBackendClient, sharedWalletService, sharedExpenseRepository, outboxService);
            }
        }

        @Nested
        class Success {

            @BeforeEach
            void setUp() {
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
                when(participants.ownerId()).thenReturn(OWNER_ID);
                when(participants.memberId()).thenReturn(MEMBER_ID);
                when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);
                when(sharedExpenseRepository.save(any(SharedExpense.class))).thenReturn(savedExpense);
                when(savedExpense.getId()).thenReturn(EXPENSE_ID);
                when(savedExpense.getAmount()).thenReturn(AMOUNT);
            }

            @Test
            void shouldReturnResponseWithCreatorWhenExpenseIsAdded() {
                SharedExpenseResponse result = sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID);

                assertEquals(new SharedExpenseResponse(null, USER_ID, USERNAME), result);
            }

            @Test
            void shouldSaveExpenseWithRequestDataWhenExpenseIsAdded() {
                sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID);

                verify(sharedExpenseRepository).save(expenseCaptor.capture());
                SharedExpense captured = expenseCaptor.getValue();
                assertEquals(AMOUNT, captured.getAmount());
                assertEquals(category, captured.getCategory());
                assertEquals(DESCRIPTION, captured.getDescription());
                assertEquals(OWNER_ID, captured.getOwnerId());
                assertEquals(MEMBER_ID, captured.getMemberId());
                assertEquals(USER_ID, captured.getCreatedByUserId());
            }

            @Test
            void shouldRemoveExpenseAmountFromWalletWhenExpenseIsAdded() {
                sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID);

                verify(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);
            }

            @Test
            void shouldHandleSpendControlWhenExpenseIsAdded() {
                sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID);

                verify(spendControlService).handleSpendControl(USER_ID, AMOUNT);
            }

            @Test
            void shouldHandleExpenseAnalysisInAddModeWhenExpenseIsAdded() {
                sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID);

                verify(expenseAnalysisService).handleExpenseAnalysis(eq(USER_ID), any(), eq(AMOUNT), eq(ExpenseAnalysisMode.ADD));
            }

            @Test
            void shouldSaveExpenseEventToOutboxWhenExpenseIsAdded() {
                sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID);

                verify(outboxService).save(eq("SharedAccountExpense"), eq(EXPENSE_ID.toString()), eq("shared-account.expense.created"), any(SharedAccountExpenseActivityEvent.class));
            }

            @Test
            void shouldNotifyAboutLargeExpenseWithBuiltExpenseWhenExpenseIsAdded() {
                sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID);

                verify(sharedExpenseRepository).save(expenseCaptor.capture());
                verify(largeExpenseNotificationService).handleLargeNotification(eq(USER_ID), same(expenseCaptor.getValue()));
            }

            @Test
            void shouldNotifyAboutLargeExpenseAfterOutboxSaveWhenExpenseIsAdded() {
                InOrder inOrder = inOrder(outboxService, largeExpenseNotificationService);

                sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID);

                inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
                inOrder.verify(largeExpenseNotificationService).handleLargeNotification(eq(USER_ID), any(SharedExpense.class));
            }

            @Test
            void shouldRemoveFromWalletBeforeSavingWhenExpenseIsAdded() {
                InOrder inOrder = inOrder(sharedWalletService, sharedExpenseRepository);

                sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID);

                inOrder.verify(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);
                inOrder.verify(sharedExpenseRepository).save(any(SharedExpense.class));
            }

            @Test
            void shouldAcceptExpenseWhenAmountEqualsOne() {
                when(sharedExpenseDto.amount()).thenReturn(BigDecimal.ONE);

                sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID);

                verify(sharedWalletService).removeBalanceFromWallet(USER_ID, BigDecimal.ONE);
            }

            @Test
            void shouldAddExpenseWhenNoLimitsExist() {
                SharedExpenseResponse result = sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID);

                assertNotNull(result);
                verify(financialPeriodService, never()).getSharedExpensesSum(any(), any(), any());
            }

            @Test
            void shouldAddExpenseWhenTotalEqualsGeneralLimit() {
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit));
                when(financialPeriodService.getSharedExpensesSum(eq(USER_ID), any(), any())).thenReturn(new BigDecimal("5.00"));
                when(limit.getAmount()).thenReturn(LIMIT_AMOUNT);

                sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID);

                verify(sharedExpenseRepository).save(any(SharedExpense.class));
            }

            @Test
            void shouldAddExpenseWhenTotalEqualsCategoryLimit() {
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit));
                when(limit.getCategory()).thenReturn(category);
                when(financialPeriodService.getSharedExpensesSum(eq(USER_ID), any(), any())).thenReturn(new BigDecimal("5.00"));
                when(limit.getAmount()).thenReturn(LIMIT_AMOUNT);

                sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID);

                verify(sharedExpenseRepository).save(any(SharedExpense.class));
            }

            @Test
            void shouldIgnoreLimitWhenLimitCategoryDiffersFromExpenseCategory() {
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit));
                when(limit.getCategory()).thenReturn(otherCategory);

                sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID);

                verify(financialPeriodService, never()).getSharedExpensesSum(any(), any(), any());
                verify(sharedExpenseRepository).save(any(SharedExpense.class));
            }
        }

        @Nested
        class EarlyFailures {

            @Test
            void shouldThrowExceptionWhenParticipantsLookupFails() {
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenThrow(new IllegalStateException());

                assertThrows(IllegalStateException.class, () -> sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID));
                verifyNoInteractions(authBackendClient, sharedWalletService, sharedExpenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenUsernameLookupFails() {
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
                when(authBackendClient.getUsername(USER_ID)).thenThrow(new IllegalStateException());

                assertThrows(IllegalStateException.class, () -> sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID));
                verifyNoInteractions(sharedWalletService, sharedExpenseRepository, outboxService);
            }
        }

        @Nested
        class LateFailures {

            @BeforeEach
            void setUp() {
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
                when(participants.ownerId()).thenReturn(OWNER_ID);
                when(participants.memberId()).thenReturn(MEMBER_ID);
                when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);
            }

            @Test
            void shouldThrowExceptionWhenWalletWithdrawalFails() {
                doThrow(new IllegalStateException()).when(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);

                assertThrows(IllegalStateException.class, () -> sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID));
                verifyNoInteractions(sharedExpenseRepository, outboxService, largeExpenseNotificationService);
            }

            @Test
            void shouldThrowExceptionWhenSavingExpenseFails() {
                when(sharedExpenseRepository.save(any(SharedExpense.class))).thenThrow(new IllegalStateException());

                assertThrows(IllegalStateException.class, () -> sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID));
                verifyNoInteractions(outboxService, largeExpenseNotificationService);
            }

            @Test
            void shouldThrowExceptionWhenOutboxSaveFails() {
                when(sharedExpenseRepository.save(any(SharedExpense.class))).thenReturn(savedExpense);
                when(savedExpense.getId()).thenReturn(EXPENSE_ID);
                when(savedExpense.getAmount()).thenReturn(AMOUNT);
                doThrow(new IllegalStateException()).when(outboxService).save(anyString(), anyString(), anyString(), any());

                assertThrows(IllegalStateException.class, () -> sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID));
                verifyNoInteractions(largeExpenseNotificationService);
            }

            @Test
            void shouldThrowExceptionWhenLargeExpenseNotificationFails() {
                when(sharedExpenseRepository.save(any(SharedExpense.class))).thenReturn(savedExpense);
                when(savedExpense.getId()).thenReturn(EXPENSE_ID);
                when(savedExpense.getAmount()).thenReturn(AMOUNT);
                doThrow(new IllegalStateException()).when(largeExpenseNotificationService).handleLargeNotification(eq(USER_ID), any(SharedExpense.class));

                assertThrows(IllegalStateException.class, () -> sharedExpenseService.addExpense(sharedExpenseRequest, USER_ID));
            }
        }
    }

    @Nested
    class EditExpense {

        @Nested
        class AsOwner {

            @BeforeEach
            void setUp() {
                when(sharedExpenseManagerService.getSharedExpenseOrThrow(EXPENSE_ID)).thenReturn(existingExpense);
                when(sharedExpenseRequest.sharedExpenseDto()).thenReturn(sharedExpenseDto);
                when(sharedExpenseDto.category()).thenReturn(category);
                when(sharedExpenseDto.amount()).thenReturn(AMOUNT);
                when(sharedExpenseDto.description()).thenReturn(DESCRIPTION);
                when(existingExpense.getOwnerId()).thenReturn(USER_ID);
                when(existingExpense.getCategory()).thenReturn(otherCategory);
                when(existingExpense.getAmount()).thenReturn(OLD_AMOUNT);
            }

            @Test
            void shouldReturnExpenseIdWhenExpenseIsEdited() {
                Long result = sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID);

                assertEquals(EXPENSE_ID, result);
            }

            @Test
            void shouldUpdateExpenseFieldsWhenExpenseIsEdited() {
                sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID);

                verify(existingExpense).setAmount(AMOUNT);
                verify(existingExpense).setCategory(category);
                verify(existingExpense).setDescription(DESCRIPTION);
            }

            @Test
            void shouldSaveExistingExpenseWhenExpenseIsEdited() {
                sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID);

                verify(sharedExpenseRepository).save(existingExpense);
            }

            @Test
            void shouldRefundOldAmountAndWithdrawNewAmountWhenExpenseIsEdited() {
                InOrder inOrder = inOrder(sharedWalletService);

                sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID);

                inOrder.verify(sharedWalletService).addBalanceToWallet(USER_ID, OLD_AMOUNT);
                inOrder.verify(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);
            }

            @Test
            void shouldHandleExpenseAnalysisInEditModeWhenExpenseIsEdited() {
                sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID);

                verify(expenseAnalysisService).handleExpenseAnalysis(eq(USER_ID), any(), eq(AMOUNT), eq(ExpenseAnalysisMode.EDIT));
            }

            @Test
            void shouldNotUseOutboxOrSpendControlWhenExpenseIsEdited() {
                sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID);

                verifyNoInteractions(outboxService, spendControlService, largeExpenseNotificationService);
            }

            @Test
            void shouldSubtractOldAmountWhenOldCategoryMatchesLimit() {
                when(existingExpense.getCategory()).thenReturn(category);
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit));
                when(limit.getCategory()).thenReturn(category);
                when(financialPeriodService.getSharedExpensesSum(eq(USER_ID), any(), any())).thenReturn(new BigDecimal("12.00"));
                when(limit.getAmount()).thenReturn(LIMIT_AMOUNT);

                sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID);

                verify(sharedExpenseRepository).save(existingExpense);
            }

            @Test
            void shouldThrowExceptionWhenOldAmountDoesNotBelongToLimitCategory() {
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit));
                when(limit.getCategory()).thenReturn(category);
                when(financialPeriodService.getSharedExpensesSum(eq(USER_ID), any(), any())).thenReturn(new BigDecimal("12.00"));
                when(limit.getAmount()).thenReturn(LIMIT_AMOUNT);

                MissingRequirementException exception = assertThrows(MissingRequirementException.class, () -> sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID));

                assertEquals("Category limit exceeded", exception.getMessage());
            }

            @Test
            void shouldThrowExceptionWhenGeneralLimitIsExceededOnEdit() {
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit));
                when(financialPeriodService.getSharedExpensesSum(eq(USER_ID), any(), any())).thenReturn(new BigDecimal("20.00"));
                when(limit.getAmount()).thenReturn(LIMIT_AMOUNT);

                MissingRequirementException exception = assertThrows(MissingRequirementException.class, () -> sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID));

                assertEquals("General limit exceeded", exception.getMessage());
            }

            @Test
            void shouldNotChangeWalletOrExpenseWhenLimitIsExceededOnEdit() {
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of(limit));
                when(financialPeriodService.getSharedExpensesSum(eq(USER_ID), any(), any())).thenReturn(new BigDecimal("20.00"));
                when(limit.getAmount()).thenReturn(LIMIT_AMOUNT);

                assertThrows(MissingRequirementException.class, () -> sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID));

                verifyNoInteractions(expenseAnalysisService, sharedWalletService, sharedExpenseRepository);
                verify(existingExpense, never()).setAmount(any(BigDecimal.class));
            }

            @Test
            void shouldThrowExceptionWhenExpenseAnalysisFailsOnEdit() {
                doThrow(new IllegalStateException()).when(expenseAnalysisService).handleExpenseAnalysis(eq(USER_ID), any(), eq(AMOUNT), eq(ExpenseAnalysisMode.EDIT));

                assertThrows(IllegalStateException.class, () -> sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID));
                verifyNoInteractions(sharedWalletService, sharedExpenseRepository);
            }

            @Test
            void shouldThrowExceptionWhenWalletRefundFails() {
                doThrow(new IllegalStateException()).when(sharedWalletService).addBalanceToWallet(USER_ID, OLD_AMOUNT);

                assertThrows(IllegalStateException.class, () -> sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID));
                verifyNoInteractions(sharedExpenseRepository);
            }

            @Test
            void shouldThrowExceptionWhenWalletWithdrawalFailsOnEdit() {
                doThrow(new IllegalStateException()).when(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);

                assertThrows(IllegalStateException.class, () -> sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID));
                verifyNoInteractions(sharedExpenseRepository);
            }

            @Test
            void shouldThrowExceptionWhenSavingEditedExpenseFails() {
                when(sharedExpenseRepository.save(existingExpense)).thenThrow(new IllegalStateException());

                assertThrows(IllegalStateException.class, () -> sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID));
            }
        }

        @Nested
        class AsMember {

            @BeforeEach
            void setUp() {
                when(sharedExpenseManagerService.getSharedExpenseOrThrow(EXPENSE_ID)).thenReturn(existingExpense);
                when(sharedExpenseRequest.sharedExpenseDto()).thenReturn(sharedExpenseDto);
                when(sharedExpenseDto.category()).thenReturn(category);
                when(sharedExpenseDto.amount()).thenReturn(AMOUNT);
                when(sharedExpenseDto.description()).thenReturn(DESCRIPTION);
                when(existingExpense.getOwnerId()).thenReturn(OTHER_USER_ID);
                when(existingExpense.getMemberId()).thenReturn(USER_ID);
            }

            @Test
            void shouldEditExpenseWhenUserIsMember() {
                when(existingExpense.getCategory()).thenReturn(otherCategory);
                when(existingExpense.getAmount()).thenReturn(OLD_AMOUNT);

                Long result = sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID);

                assertEquals(EXPENSE_ID, result);
                verify(sharedExpenseRepository).save(existingExpense);
            }
        }

        @Nested
        class NotParticipant {

            @BeforeEach
            void setUp() {
                when(sharedExpenseManagerService.getSharedExpenseOrThrow(EXPENSE_ID)).thenReturn(existingExpense);
                when(sharedExpenseRequest.sharedExpenseDto()).thenReturn(sharedExpenseDto);
                when(sharedExpenseDto.category()).thenReturn(category);
                when(sharedExpenseDto.amount()).thenReturn(AMOUNT);
                when(sharedExpenseDto.description()).thenReturn(DESCRIPTION);
                when(existingExpense.getOwnerId()).thenReturn(OTHER_USER_ID);
                when(existingExpense.getMemberId()).thenReturn(OTHER_USER_ID);
            }

            @Test
            void shouldThrowExceptionWhenUserIsNeitherOwnerNorMember() {
                RequestedEntityNotFoundException exception = assertThrows(RequestedEntityNotFoundException.class, () -> sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID));

                assertEquals("Expense not found for this user", exception.getMessage());
            }

            @Test
            void shouldNotChangeAnythingWhenUserIsNeitherOwnerNorMember() {
                assertThrows(RequestedEntityNotFoundException.class, () -> sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID));

                verifyNoInteractions(sharedLimitRepository, expenseAnalysisService, sharedWalletService, sharedExpenseRepository);
            }
        }

        @Nested
        class ManagerFailure {

            @Test
            void shouldThrowExceptionWhenExpenseDoesNotExist() {
                when(sharedExpenseManagerService.getSharedExpenseOrThrow(EXPENSE_ID)).thenThrow(new RequestedEntityNotFoundException("Expense not found"));

                assertThrows(RequestedEntityNotFoundException.class, () -> sharedExpenseService.editExpense(sharedExpenseRequest, USER_ID, EXPENSE_ID));
                verifyNoInteractions(sharedLimitRepository, expenseAnalysisService, sharedWalletService, sharedExpenseRepository);
            }
        }
    }

    @Nested
    class GetExpense {

        @Test
        void shouldReturnEmptyListWhenNoExpensesExist() {
            List<SharedExpenseDto> result = sharedExpenseService.getExpense(USER_ID);

            assertTrue(result.isEmpty());
            verifyNoInteractions(authBackendClient, sharedExpenseMapper);
        }

        @Test
        void shouldReturnMappedExpenseWhenSingleExpenseExists() {
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenReturn(List.of(firstExpense));
            when(firstExpense.getCreatedByUserId()).thenReturn(USER_ID);
            when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);
            when(sharedExpenseMapper.mapToDto(firstExpense, USERNAME)).thenReturn(firstMappedDto);

            List<SharedExpenseDto> result = sharedExpenseService.getExpense(USER_ID);

            assertEquals(List.of(firstMappedDto), result);
        }

        @Test
        void shouldMapEachExpenseWithItsCreatorUsernameWhenCreatorsDiffer() {
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenReturn(List.of(firstExpense, secondExpense));
            when(firstExpense.getCreatedByUserId()).thenReturn(USER_ID);
            when(secondExpense.getCreatedByUserId()).thenReturn(OTHER_USER_ID);
            when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);
            when(authBackendClient.getUsername(OTHER_USER_ID)).thenReturn(OTHER_USERNAME);
            when(sharedExpenseMapper.mapToDto(firstExpense, USERNAME)).thenReturn(firstMappedDto);
            when(sharedExpenseMapper.mapToDto(secondExpense, OTHER_USERNAME)).thenReturn(secondMappedDto);

            List<SharedExpenseDto> result = sharedExpenseService.getExpense(USER_ID);

            assertEquals(List.of(firstMappedDto, secondMappedDto), result);
        }

        @Test
        void shouldFetchUsernameOnceWhenExpensesShareCreator() {
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenReturn(List.of(firstExpense, secondExpense));
            when(firstExpense.getCreatedByUserId()).thenReturn(USER_ID);
            when(secondExpense.getCreatedByUserId()).thenReturn(USER_ID);
            when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);
            when(sharedExpenseMapper.mapToDto(firstExpense, USERNAME)).thenReturn(firstMappedDto);
            when(sharedExpenseMapper.mapToDto(secondExpense, USERNAME)).thenReturn(secondMappedDto);

            List<SharedExpenseDto> result = sharedExpenseService.getExpense(USER_ID);

            verify(authBackendClient, times(1)).getUsername(USER_ID);
            assertEquals(List.of(firstMappedDto, secondMappedDto), result);
        }

        @Test
        void shouldThrowExceptionWhenCreatorUsernameIsNull() {
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenReturn(List.of(firstExpense));
            when(firstExpense.getCreatedByUserId()).thenReturn(USER_ID);

            assertThrows(NullPointerException.class, () -> sharedExpenseService.getExpense(USER_ID));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> sharedExpenseService.getExpense(USER_ID));
        }

        @Test
        void shouldThrowExceptionWhenUsernameLookupFails() {
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenReturn(List.of(firstExpense));
            when(firstExpense.getCreatedByUserId()).thenReturn(USER_ID);
            when(authBackendClient.getUsername(USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> sharedExpenseService.getExpense(USER_ID));
            verifyNoInteractions(sharedExpenseMapper);
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenReturn(List.of(firstExpense));
            when(firstExpense.getCreatedByUserId()).thenReturn(USER_ID);
            when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);
            when(sharedExpenseMapper.mapToDto(firstExpense, USERNAME)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> sharedExpenseService.getExpense(USER_ID));
        }
    }

    @Nested
    class DeleteExpense {

        @Test
        void shouldRefundAmountToWalletWhenExpenseExists() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, USER_ID)).thenReturn(Optional.of(existingExpense));
            when(existingExpense.getAmount()).thenReturn(AMOUNT);

            sharedExpenseService.deleteExpense(EXPENSE_ID, USER_ID);

            verify(sharedWalletService).addBalanceToWallet(USER_ID, AMOUNT);
        }

        @Test
        void shouldDeleteExpenseWhenExpenseExists() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, USER_ID)).thenReturn(Optional.of(existingExpense));
            when(existingExpense.getAmount()).thenReturn(AMOUNT);

            sharedExpenseService.deleteExpense(EXPENSE_ID, USER_ID);

            verify(sharedExpenseRepository).delete(existingExpense);
        }

        @Test
        void shouldRefundWalletBeforeDeletingWhenExpenseExists() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, USER_ID)).thenReturn(Optional.of(existingExpense));
            when(existingExpense.getAmount()).thenReturn(AMOUNT);
            InOrder inOrder = inOrder(sharedWalletService, sharedExpenseRepository);

            sharedExpenseService.deleteExpense(EXPENSE_ID, USER_ID);

            inOrder.verify(sharedWalletService).addBalanceToWallet(USER_ID, AMOUNT);
            inOrder.verify(sharedExpenseRepository).delete(existingExpense);
        }

        @Test
        void shouldThrowExceptionWhenExpenseDoesNotExist() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, USER_ID)).thenReturn(Optional.empty());

            RequestedEntityNotFoundException exception = assertThrows(RequestedEntityNotFoundException.class, () -> sharedExpenseService.deleteExpense(EXPENSE_ID, USER_ID));

            assertEquals("Expense not found", exception.getMessage());
        }

        @Test
        void shouldNotTouchWalletOrDeleteWhenExpenseDoesNotExist() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> sharedExpenseService.deleteExpense(EXPENSE_ID, USER_ID));

            verifyNoInteractions(sharedWalletService);
            verify(sharedExpenseRepository, never()).delete(any(SharedExpense.class));
        }

        @Test
        void shouldNotDeleteExpenseWhenWalletRefundFails() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, USER_ID)).thenReturn(Optional.of(existingExpense));
            when(existingExpense.getAmount()).thenReturn(AMOUNT);
            doThrow(new IllegalStateException()).when(sharedWalletService).addBalanceToWallet(USER_ID, AMOUNT);

            assertThrows(IllegalStateException.class, () -> sharedExpenseService.deleteExpense(EXPENSE_ID, USER_ID));

            verify(sharedExpenseRepository, never()).delete(any(SharedExpense.class));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, USER_ID)).thenReturn(Optional.of(existingExpense));
            when(existingExpense.getAmount()).thenReturn(AMOUNT);
            doThrow(new IllegalStateException()).when(sharedExpenseRepository).delete(existingExpense);

            assertThrows(IllegalStateException.class, () -> sharedExpenseService.deleteExpense(EXPENSE_ID, USER_ID));
        }
    }
}