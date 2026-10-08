package com.finovara.financeservice.sharedaccount.expense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.contracts.util.model.ExpenseCategory;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

@ExtendWith(MockitoExtension.class)
class SharedExpenseServiceTest {

    private static final Long OWNER_ID = 10L;
    private static final Long MEMBER_ID = 20L;
    private static final Long OUTSIDER_ID = 99L;
    private static final Long EXPENSE_ID = 100L;
    private static final String USERNAME = "john";
    private static final String OTHER_USERNAME = "anna";
    private static final String AGGREGATE = "SharedAccountExpense";
    private static final String TOPIC = "shared-account.activity";
    private static final String DESCRIPTION = "Groceries";
    private static final BigDecimal AMOUNT = new BigDecimal("50.00");
    private static final BigDecimal OLD_AMOUNT = new BigDecimal("20.00");
    private static final BigDecimal LIMIT_AMOUNT = new BigDecimal("100.00");
    private static final ExpenseCategory CATEGORY = ExpenseCategory.values()[0];
    private static final ExpenseCategory OTHER_CATEGORY = ExpenseCategory.values()[1];

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
    private SharedExpenseRequest expenseRequest;

    @Mock
    private SharedExpenseDto expenseDto;

    @Mock
    private SharedAccountParticipantsResponse participantsResponse;

    @Mock
    private SharedExpense savedExpense;

    @Mock
    private SharedExpense existingExpense;

    @Mock
    private SharedLimit limit;

    @Mock
    private SharedLimit secondLimit;

    @Captor
    private ArgumentCaptor<SharedExpense> expenseCaptor;

    @Captor
    private ArgumentCaptor<SharedAccountActivityLogEvent> eventCaptor;

    @InjectMocks
    private SharedExpenseService service;

    private void stubRequest(BigDecimal amount) {
        when(expenseRequest.sharedExpenseDto()).thenReturn(expenseDto);
        when(expenseDto.category()).thenReturn(CATEGORY);
        when(expenseDto.amount()).thenReturn(amount);
        when(expenseDto.description()).thenReturn(DESCRIPTION);
    }

    private void stubParticipants(Long userId) {
        when(sharedAccountParticipantsService.getParticipants(userId)).thenReturn(participantsResponse);
        when(participantsResponse.ownerId()).thenReturn(OWNER_ID);
        when(participantsResponse.memberId()).thenReturn(MEMBER_ID);
    }

    private void stubLimit(SharedLimit target, ExpenseCategory category, BigDecimal spent) {
        when(target.getCategory()).thenReturn(category);
        when(target.getAmount()).thenReturn(LIMIT_AMOUNT);
        when(financialPeriodService.getSharedExpensesSum(OWNER_ID, target.getPeriodType(), target.getCategory()))
                .thenReturn(spent);
    }

    private void verifyOutboxEvent(Long userId, SharedAccountActivityLogType type) {
        verify(outboxService).save(eq(AGGREGATE), eq(EXPENSE_ID.toString()), eq(TOPIC), eventCaptor.capture());
        SharedAccountActivityLogEvent event = eventCaptor.getValue();
        assertEquals(OWNER_ID, event.ownerId());
        assertEquals(MEMBER_ID, event.memberId());
        assertEquals(userId, event.userId());
        assertEquals(EXPENSE_ID, event.targetId());
        assertEquals(type, event.type());
        assertNotNull(event.createdAt());
    }

    @Nested
    class AddExpense {

        private void stubSuccessfulAdd() {
            stubRequest(AMOUNT);
            stubParticipants(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(sharedExpenseRepository.save(any(SharedExpense.class))).thenReturn(savedExpense);
            when(savedExpense.getId()).thenReturn(EXPENSE_ID);
        }

        @Test
        void shouldReturnResponseWhenExpenseIsAdded() {
            stubSuccessfulAdd();

            SharedExpenseResponse result = service.addExpense(expenseRequest, OWNER_ID);

            verify(sharedExpenseRepository).save(expenseCaptor.capture());
            assertEquals(new SharedExpenseResponse(expenseCaptor.getValue().getId(), OWNER_ID, USERNAME), result);
        }

        @Test
        void shouldSaveExpenseWithRequestAndParticipantDataWhenExpenseIsAdded() {
            stubSuccessfulAdd();

            service.addExpense(expenseRequest, OWNER_ID);

            verify(sharedExpenseRepository).save(expenseCaptor.capture());
            SharedExpense saved = expenseCaptor.getValue();
            assertEquals(AMOUNT, saved.getAmount());
            assertEquals(CATEGORY, saved.getCategory());
            assertEquals(DESCRIPTION, saved.getDescription());
            assertEquals(OWNER_ID, saved.getOwnerId());
            assertEquals(MEMBER_ID, saved.getMemberId());
            assertEquals(OWNER_ID, saved.getCreatedByUserId());
            assertEquals(LocalDate.now(), saved.getCreatedAt());
        }

        @Test
        void shouldSaveExpenseWithMemberAsCreatorWhenMemberAddsExpense() {
            stubRequest(AMOUNT);
            stubParticipants(MEMBER_ID);
            when(authBackendClient.getUsername(MEMBER_ID)).thenReturn(OTHER_USERNAME);
            when(sharedExpenseRepository.save(any(SharedExpense.class))).thenReturn(savedExpense);
            when(savedExpense.getId()).thenReturn(EXPENSE_ID);

            service.addExpense(expenseRequest, MEMBER_ID);

            verify(sharedExpenseRepository).save(expenseCaptor.capture());
            assertEquals(MEMBER_ID, expenseCaptor.getValue().getCreatedByUserId());
            verify(sharedWalletService).removeBalanceFromWallet(MEMBER_ID, AMOUNT);
        }

        @Test
        void shouldRemoveAmountFromWalletWhenExpenseIsAdded() {
            stubSuccessfulAdd();

            service.addExpense(expenseRequest, OWNER_ID);

            verify(sharedWalletService).removeBalanceFromWallet(OWNER_ID, AMOUNT);
        }

        @Test
        void shouldHandleSpendControlAndAnalysisInAddModeWhenExpenseIsAdded() {
            stubSuccessfulAdd();

            service.addExpense(expenseRequest, OWNER_ID);

            verify(spendControlService).handleSpendControl(OWNER_ID, AMOUNT);
            verify(expenseAnalysisService).handleExpenseAnalysis(eq(OWNER_ID), any(), eq(AMOUNT), eq(ExpenseAnalysisMode.ADD));
        }

        @Test
        void shouldSaveOutboxEventWhenExpenseIsAdded() {
            stubSuccessfulAdd();

            service.addExpense(expenseRequest, OWNER_ID);

            verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.EXPENSE_CREATED);
        }

        @Test
        void shouldNotifyAboutLargeExpenseWhenExpenseIsAdded() {
            stubSuccessfulAdd();

            service.addExpense(expenseRequest, OWNER_ID);

            verify(sharedExpenseRepository).save(expenseCaptor.capture());
            verify(largeExpenseNotificationService).handleLargeNotification(OWNER_ID, expenseCaptor.getValue());
        }

        @Test
        void shouldProcessStepsInOrderWhenExpenseIsAdded() {
            stubSuccessfulAdd();

            service.addExpense(expenseRequest, OWNER_ID);

            InOrder inOrder = inOrder(spendControlService, sharedLimitRepository, expenseAnalysisService,
                    sharedWalletService, sharedExpenseRepository, outboxService, largeExpenseNotificationService);
            inOrder.verify(spendControlService).handleSpendControl(OWNER_ID, AMOUNT);
            inOrder.verify(sharedLimitRepository).findAllByUserId(OWNER_ID);
            inOrder.verify(expenseAnalysisService).handleExpenseAnalysis(eq(OWNER_ID), any(), eq(AMOUNT), eq(ExpenseAnalysisMode.ADD));
            inOrder.verify(sharedWalletService).removeBalanceFromWallet(OWNER_ID, AMOUNT);
            inOrder.verify(sharedExpenseRepository).save(any(SharedExpense.class));
            inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
            inOrder.verify(largeExpenseNotificationService).handleLargeNotification(eq(OWNER_ID), any(SharedExpense.class));
        }

        @Test
        void shouldAddExpenseWhenAmountEqualsMinimum() {
            stubRequest(BigDecimal.ONE);
            stubParticipants(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(sharedExpenseRepository.save(any(SharedExpense.class))).thenReturn(savedExpense);
            when(savedExpense.getId()).thenReturn(EXPENSE_ID);

            service.addExpense(expenseRequest, OWNER_ID);

            verify(sharedWalletService).removeBalanceFromWallet(OWNER_ID, BigDecimal.ONE);
        }

        @Test
        void shouldAddExpenseWhenGeneralLimitIsNotExceeded() {
            stubSuccessfulAdd();
            when(sharedLimitRepository.findAllByUserId(OWNER_ID)).thenReturn(List.of(limit));
            stubLimit(limit, null, new BigDecimal("30.00"));

            service.addExpense(expenseRequest, OWNER_ID);

            verify(sharedExpenseRepository).save(any(SharedExpense.class));
        }

        @Test
        void shouldAddExpenseWhenTotalEqualsLimitAmount() {
            stubSuccessfulAdd();
            when(sharedLimitRepository.findAllByUserId(OWNER_ID)).thenReturn(List.of(limit));
            stubLimit(limit, CATEGORY, new BigDecimal("50.00"));

            service.addExpense(expenseRequest, OWNER_ID);

            verify(sharedExpenseRepository).save(any(SharedExpense.class));
        }

        @Test
        void shouldIgnoreLimitWhenLimitCategoryDiffersFromExpenseCategory() {
            stubSuccessfulAdd();
            when(sharedLimitRepository.findAllByUserId(OWNER_ID)).thenReturn(List.of(limit));
            when(limit.getCategory()).thenReturn(OTHER_CATEGORY);

            service.addExpense(expenseRequest, OWNER_ID);

            verifyNoInteractions(financialPeriodService);
            verify(sharedExpenseRepository).save(any(SharedExpense.class));
        }

        @Test
        void shouldCheckEveryLimitWhenMultipleLimitsExist() {
            stubSuccessfulAdd();
            when(sharedLimitRepository.findAllByUserId(OWNER_ID)).thenReturn(List.of(limit, secondLimit));
            stubLimit(limit, null, new BigDecimal("10.00"));
            stubLimit(secondLimit, CATEGORY, new BigDecimal("20.00"));

            service.addExpense(expenseRequest, OWNER_ID);

            verify(financialPeriodService).getSharedExpensesSum(OWNER_ID, limit.getPeriodType(), null);
            verify(financialPeriodService).getSharedExpensesSum(OWNER_ID, secondLimit.getPeriodType(), CATEGORY);
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "0.99", "-1", "-100.50"})
        void shouldThrowExceptionWhenAmountIsBelowMinimum(BigDecimal amount) {
            stubRequest(amount);

            assertThrows(InvalidInputException.class, () -> service.addExpense(expenseRequest, OWNER_ID));

            verifyNoInteractions(spendControlService, sharedLimitRepository, expenseAnalysisService, sharedWalletService,
                    sharedExpenseRepository, outboxService, largeExpenseNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenRequestIsNull() {
            assertThrows(NullPointerException.class, () -> service.addExpense(null, OWNER_ID));

            verifyNoInteractions(spendControlService, sharedExpenseRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenExpenseDtoIsNull() {
            assertThrows(NullPointerException.class, () -> service.addExpense(expenseRequest, OWNER_ID));

            verifyNoInteractions(spendControlService, sharedExpenseRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            when(expenseRequest.sharedExpenseDto()).thenReturn(expenseDto);

            assertThrows(NullPointerException.class, () -> service.addExpense(expenseRequest, OWNER_ID));

            verifyNoInteractions(spendControlService, sharedExpenseRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenGeneralLimitIsExceeded() {
            stubRequest(AMOUNT);
            when(sharedLimitRepository.findAllByUserId(OWNER_ID)).thenReturn(List.of(limit));
            stubLimit(limit, null, new BigDecimal("60.00"));

            assertThrows(MissingRequirementException.class, () -> service.addExpense(expenseRequest, OWNER_ID));

            verifyNoInteractions(expenseAnalysisService, sharedWalletService, sharedExpenseRepository, outboxService,
                    largeExpenseNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenCategoryLimitIsExceeded() {
            stubRequest(AMOUNT);
            when(sharedLimitRepository.findAllByUserId(OWNER_ID)).thenReturn(List.of(limit));
            stubLimit(limit, CATEGORY, new BigDecimal("60.00"));

            assertThrows(MissingRequirementException.class, () -> service.addExpense(expenseRequest, OWNER_ID));

            verifyNoInteractions(expenseAnalysisService, sharedWalletService, sharedExpenseRepository, outboxService,
                    largeExpenseNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenSecondLimitIsExceeded() {
            stubRequest(AMOUNT);
            when(sharedLimitRepository.findAllByUserId(OWNER_ID)).thenReturn(List.of(limit, secondLimit));
            stubLimit(limit, null, new BigDecimal("10.00"));
            stubLimit(secondLimit, CATEGORY, new BigDecimal("60.00"));

            assertThrows(MissingRequirementException.class, () -> service.addExpense(expenseRequest, OWNER_ID));

            verifyNoInteractions(sharedWalletService, sharedExpenseRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenSpendControlFails() {
            stubRequest(AMOUNT);
            doThrow(new IllegalStateException("spend control")).when(spendControlService).handleSpendControl(OWNER_ID, AMOUNT);

            assertThrows(IllegalStateException.class, () -> service.addExpense(expenseRequest, OWNER_ID));

            verifyNoInteractions(sharedLimitRepository, expenseAnalysisService, sharedWalletService, sharedExpenseRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenExpenseAnalysisFails() {
            stubRequest(AMOUNT);
            doThrow(new IllegalStateException("analysis")).when(expenseAnalysisService)
                    .handleExpenseAnalysis(eq(OWNER_ID), any(), eq(AMOUNT), eq(ExpenseAnalysisMode.ADD));

            assertThrows(IllegalStateException.class, () -> service.addExpense(expenseRequest, OWNER_ID));

            verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedExpenseRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsServiceFails() {
            stubRequest(AMOUNT);
            when(sharedAccountParticipantsService.getParticipants(OWNER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("participants not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.addExpense(expenseRequest, OWNER_ID));

            verifyNoInteractions(authBackendClient, sharedWalletService, sharedExpenseRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenWalletUpdateFails() {
            stubRequest(AMOUNT);
            stubParticipants(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            doThrow(new IllegalStateException("wallet failed")).when(sharedWalletService)
                    .removeBalanceFromWallet(OWNER_ID, AMOUNT);

            assertThrows(IllegalStateException.class, () -> service.addExpense(expenseRequest, OWNER_ID));

            verifyNoInteractions(sharedExpenseRepository, outboxService, largeExpenseNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            stubRequest(AMOUNT);
            stubParticipants(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(sharedExpenseRepository.save(any(SharedExpense.class))).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.addExpense(expenseRequest, OWNER_ID));

            verifyNoInteractions(outboxService, largeExpenseNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenOutboxServiceFails() {
            stubSuccessfulAdd();
            doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                    .save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class, () -> service.addExpense(expenseRequest, OWNER_ID));

            verifyNoInteractions(largeExpenseNotificationService);
        }

        @Test
        void shouldThrowExceptionWhenLargeExpenseNotificationFails() {
            stubSuccessfulAdd();
            doThrow(new IllegalStateException("notification failed")).when(largeExpenseNotificationService)
                    .handleLargeNotification(eq(OWNER_ID), any(SharedExpense.class));

            assertThrows(IllegalStateException.class, () -> service.addExpense(expenseRequest, OWNER_ID));
        }
    }

    @Nested
    class EditExpense {

        private void stubExistingExpense() {
            when(sharedExpenseManagerService.getSharedExpenseOrThrow(EXPENSE_ID)).thenReturn(existingExpense);
            when(existingExpense.getOwnerId()).thenReturn(OWNER_ID);
            when(existingExpense.getCategory()).thenReturn(OTHER_CATEGORY);
            when(existingExpense.getAmount()).thenReturn(OLD_AMOUNT);
        }

        private void stubSuccessfulEdit() {
            stubRequest(AMOUNT);
            stubExistingExpense();
            stubParticipants(OWNER_ID);
            when(sharedExpenseRepository.save(existingExpense)).thenReturn(existingExpense);
            when(existingExpense.getId()).thenReturn(EXPENSE_ID);
        }

        @Test
        void shouldReturnExpenseIdWhenOwnerEditsExpense() {
            stubSuccessfulEdit();

            Long result = service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID);

            assertEquals(EXPENSE_ID, result);
        }

        @Test
        void shouldReturnExpenseIdWhenMemberEditsExpense() {
            stubRequest(AMOUNT);
            when(sharedExpenseManagerService.getSharedExpenseOrThrow(EXPENSE_ID)).thenReturn(existingExpense);
            when(existingExpense.getOwnerId()).thenReturn(OWNER_ID);
            when(existingExpense.getMemberId()).thenReturn(MEMBER_ID);
            when(existingExpense.getCategory()).thenReturn(OTHER_CATEGORY);
            when(existingExpense.getAmount()).thenReturn(OLD_AMOUNT);
            stubParticipants(MEMBER_ID);
            when(sharedExpenseRepository.save(existingExpense)).thenReturn(existingExpense);
            when(existingExpense.getId()).thenReturn(EXPENSE_ID);

            Long result = service.editExpense(expenseRequest, MEMBER_ID, EXPENSE_ID);

            assertEquals(EXPENSE_ID, result);
            verifyOutboxEvent(MEMBER_ID, SharedAccountActivityLogType.EXPENSE_EDITED);
        }

        @Test
        void shouldUpdateExpenseFieldsWhenExpenseIsEdited() {
            stubSuccessfulEdit();

            service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID);

            verify(existingExpense).setAmount(AMOUNT);
            verify(existingExpense).setCategory(CATEGORY);
            verify(existingExpense).setDescription(DESCRIPTION);
            verify(sharedExpenseRepository).save(existingExpense);
        }

        @Test
        void shouldReturnOldAmountAndChargeNewAmountWhenExpenseIsEdited() {
            stubSuccessfulEdit();

            service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID);

            InOrder inOrder = inOrder(sharedWalletService);
            inOrder.verify(sharedWalletService).addBalanceToWallet(OWNER_ID, OLD_AMOUNT);
            inOrder.verify(sharedWalletService).removeBalanceFromWallet(OWNER_ID, AMOUNT);
        }

        @Test
        void shouldHandleExpenseAnalysisInEditModeWhenExpenseIsEdited() {
            stubSuccessfulEdit();

            service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID);

            verify(expenseAnalysisService).handleExpenseAnalysis(eq(OWNER_ID), any(), eq(AMOUNT), eq(ExpenseAnalysisMode.EDIT));
            verifyNoInteractions(spendControlService, largeExpenseNotificationService);
        }

        @Test
        void shouldSaveOutboxEventWhenExpenseIsEdited() {
            stubSuccessfulEdit();

            service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID);

            verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.EXPENSE_EDITED);
        }

        @Test
        void shouldNotSubtractOldAmountWhenOldCategoryDoesNotMatchLimitAndLimitIsNotExceeded() {
            stubSuccessfulEdit();
            when(sharedLimitRepository.findAllByUserId(OWNER_ID)).thenReturn(List.of(limit));
            stubLimit(limit, CATEGORY, new BigDecimal("40.00"));

            service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID);

            verify(sharedExpenseRepository).save(existingExpense);
        }

        @Test
        void shouldThrowExceptionWhenOldAmountIsNotCountedAndNewTotalExceedsLimit() {
            stubRequest(AMOUNT);
            stubExistingExpense();
            when(sharedLimitRepository.findAllByUserId(OWNER_ID)).thenReturn(List.of(limit));
            stubLimit(limit, CATEGORY, new BigDecimal("60.00"));

            assertThrows(MissingRequirementException.class,
                    () -> service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID));

            verifyNoInteractions(expenseAnalysisService, sharedWalletService, outboxService);
        }

        @Test
        void shouldSubtractOldAmountWhenOldCategoryMatchesLimit() {
            stubRequest(AMOUNT);
            when(sharedExpenseManagerService.getSharedExpenseOrThrow(EXPENSE_ID)).thenReturn(existingExpense);
            when(existingExpense.getOwnerId()).thenReturn(OWNER_ID);
            when(existingExpense.getCategory()).thenReturn(CATEGORY);
            when(existingExpense.getAmount()).thenReturn(OLD_AMOUNT);
            stubParticipants(OWNER_ID);
            when(sharedExpenseRepository.save(existingExpense)).thenReturn(existingExpense);
            when(existingExpense.getId()).thenReturn(EXPENSE_ID);
            when(sharedLimitRepository.findAllByUserId(OWNER_ID)).thenReturn(List.of(limit));
            stubLimit(limit, CATEGORY, new BigDecimal("70.00"));

            service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID);

            verify(sharedExpenseRepository).save(existingExpense);
        }

        @Test
        void shouldThrowExceptionWhenEditedAmountExceedsGeneralLimit() {
            stubRequest(AMOUNT);
            stubExistingExpense();
            when(sharedLimitRepository.findAllByUserId(OWNER_ID)).thenReturn(List.of(limit));
            stubLimit(limit, null, new BigDecimal("100.00"));

            assertThrows(MissingRequirementException.class,
                    () -> service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID));

            verifyNoInteractions(expenseAnalysisService, sharedWalletService, outboxService);
        }

        @Test
        void shouldIgnoreLimitWhenNewCategoryDoesNotMatchLimitCategory() {
            stubSuccessfulEdit();
            when(sharedLimitRepository.findAllByUserId(OWNER_ID)).thenReturn(List.of(limit));
            when(limit.getCategory()).thenReturn(OTHER_CATEGORY);

            service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID);

            verifyNoInteractions(financialPeriodService);
        }

        @Test
        void shouldThrowExceptionWhenUserIsNeitherOwnerNorMember() {
            stubRequest(AMOUNT);
            when(sharedExpenseManagerService.getSharedExpenseOrThrow(EXPENSE_ID)).thenReturn(existingExpense);
            when(existingExpense.getOwnerId()).thenReturn(OWNER_ID);
            when(existingExpense.getMemberId()).thenReturn(MEMBER_ID);

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.editExpense(expenseRequest, OUTSIDER_ID, EXPENSE_ID));

            verifyNoInteractions(sharedLimitRepository, expenseAnalysisService, sharedWalletService,
                    sharedExpenseRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenExpenseDoesNotExist() {
            when(sharedExpenseManagerService.getSharedExpenseOrThrow(EXPENSE_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Expense not found"));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID));

            verifyNoInteractions(expenseAnalysisService, sharedWalletService, sharedExpenseRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRequestIsNull() {
            when(sharedExpenseManagerService.getSharedExpenseOrThrow(EXPENSE_ID)).thenReturn(existingExpense);

            assertThrows(NullPointerException.class, () -> service.editExpense(null, OWNER_ID, EXPENSE_ID));

            verifyNoInteractions(expenseAnalysisService, sharedWalletService, sharedExpenseRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenExpenseAnalysisFails() {
            stubRequest(AMOUNT);
            stubExistingExpense();
            doThrow(new IllegalStateException("analysis")).when(expenseAnalysisService)
                    .handleExpenseAnalysis(eq(OWNER_ID), any(), eq(AMOUNT), eq(ExpenseAnalysisMode.EDIT));

            assertThrows(IllegalStateException.class, () -> service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID));

            verifyNoInteractions(sharedWalletService, sharedExpenseRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAddingBalanceToWalletFails() {
            stubRequest(AMOUNT);
            stubExistingExpense();
            doThrow(new IllegalStateException("wallet failed")).when(sharedWalletService)
                    .addBalanceToWallet(OWNER_ID, OLD_AMOUNT);

            assertThrows(IllegalStateException.class, () -> service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID));

            verify(sharedWalletService, never()).removeBalanceFromWallet(any(Long.class), any(BigDecimal.class));
            verifyNoInteractions(sharedExpenseRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRemovingBalanceFromWalletFails() {
            stubRequest(AMOUNT);
            stubExistingExpense();
            doThrow(new IllegalStateException("wallet failed")).when(sharedWalletService)
                    .removeBalanceFromWallet(OWNER_ID, AMOUNT);

            assertThrows(IllegalStateException.class, () -> service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID));

            verifyNoInteractions(sharedExpenseRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsServiceFails() {
            stubRequest(AMOUNT);
            stubExistingExpense();
            when(sharedAccountParticipantsService.getParticipants(OWNER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("participants not found"));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID));

            verify(existingExpense, never()).setAmount(any());
            verifyNoInteractions(sharedExpenseRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenOutboxServiceFails() {
            stubSuccessfulEdit();
            doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                    .save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class, () -> service.editExpense(expenseRequest, OWNER_ID, EXPENSE_ID));
        }
    }

    @Nested
    class GetExpense {

        @Test
        void shouldReturnEmptyListWhenNoExpensesExist() {
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of());

            List<SharedExpenseDto> result = service.getExpense(OWNER_ID);

            assertTrue(result.isEmpty());
            verifyNoInteractions(authBackendClient, sharedExpenseMapper);
        }

        @Test
        void shouldReturnMappedExpenseWithUsernameWhenSingleExpenseExists() {
            SharedExpenseDto mappedDto = mock(SharedExpenseDto.class);
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingExpense));
            when(existingExpense.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(sharedExpenseMapper.mapToDto(existingExpense, USERNAME)).thenReturn(mappedDto);

            List<SharedExpenseDto> result = service.getExpense(OWNER_ID);

            assertEquals(List.of(mappedDto), result);
        }

        @Test
        void shouldMapEachExpenseWithItsCreatorUsernameWhenCreatorsDiffer() {
            SharedExpenseDto firstDto = mock(SharedExpenseDto.class);
            SharedExpenseDto secondDto = mock(SharedExpenseDto.class);
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(OWNER_ID))
                    .thenReturn(List.of(existingExpense, savedExpense));
            when(existingExpense.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(savedExpense.getCreatedByUserId()).thenReturn(MEMBER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(authBackendClient.getUsername(MEMBER_ID)).thenReturn(OTHER_USERNAME);
            when(sharedExpenseMapper.mapToDto(existingExpense, USERNAME)).thenReturn(firstDto);
            when(sharedExpenseMapper.mapToDto(savedExpense, OTHER_USERNAME)).thenReturn(secondDto);

            List<SharedExpenseDto> result = service.getExpense(OWNER_ID);

            assertEquals(List.of(firstDto, secondDto), result);
        }

        @Test
        void shouldFetchUsernameOnceWhenExpensesShareSameCreator() {
            SharedExpenseDto firstDto = mock(SharedExpenseDto.class);
            SharedExpenseDto secondDto = mock(SharedExpenseDto.class);
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(OWNER_ID))
                    .thenReturn(List.of(existingExpense, savedExpense));
            when(existingExpense.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(savedExpense.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(sharedExpenseMapper.mapToDto(existingExpense, USERNAME)).thenReturn(firstDto);
            when(sharedExpenseMapper.mapToDto(savedExpense, USERNAME)).thenReturn(secondDto);

            List<SharedExpenseDto> result = service.getExpense(OWNER_ID);

            assertEquals(2, result.size());
            verify(authBackendClient).getUsername(OWNER_ID);
        }

        @Test
        void shouldThrowExceptionWhenUsernameIsNull() {
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingExpense));
            when(existingExpense.getCreatedByUserId()).thenReturn(OWNER_ID);

            assertThrows(NullPointerException.class, () -> service.getExpense(OWNER_ID));

            verifyNoInteractions(sharedExpenseMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(OWNER_ID))
                    .thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.getExpense(OWNER_ID));

            verifyNoInteractions(authBackendClient, sharedExpenseMapper);
        }

        @Test
        void shouldThrowExceptionWhenAuthBackendClientFails() {
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingExpense));
            when(existingExpense.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenThrow(new IllegalStateException("auth down"));

            assertThrows(IllegalStateException.class, () -> service.getExpense(OWNER_ID));

            verifyNoInteractions(sharedExpenseMapper);
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingExpense));
            when(existingExpense.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(sharedExpenseMapper.mapToDto(existingExpense, USERNAME)).thenThrow(new IllegalArgumentException("mapping failed"));

            assertThrows(IllegalArgumentException.class, () -> service.getExpense(OWNER_ID));
        }
    }

    @Nested
    class DeleteExpense {

        private void stubSuccessfulDelete() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, OWNER_ID))
                    .thenReturn(Optional.of(existingExpense));
            when(existingExpense.getAmount()).thenReturn(OLD_AMOUNT);
            when(existingExpense.getId()).thenReturn(EXPENSE_ID);
            stubParticipants(OWNER_ID);
        }

        @Test
        void shouldDeleteExpenseWhenExpenseExists() {
            stubSuccessfulDelete();

            service.deleteExpense(EXPENSE_ID, OWNER_ID);

            verify(sharedExpenseRepository).delete(existingExpense);
        }

        @Test
        void shouldReturnExpenseAmountToWalletWhenExpenseIsDeleted() {
            stubSuccessfulDelete();

            service.deleteExpense(EXPENSE_ID, OWNER_ID);

            verify(sharedWalletService).addBalanceToWallet(OWNER_ID, OLD_AMOUNT);
        }

        @Test
        void shouldSaveOutboxEventWhenExpenseIsDeleted() {
            stubSuccessfulDelete();

            service.deleteExpense(EXPENSE_ID, OWNER_ID);

            verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.EXPENSE_DELETED);
        }

        @Test
        void shouldUpdateWalletBeforeDeletingAndSavingEventWhenExpenseIsDeleted() {
            stubSuccessfulDelete();

            service.deleteExpense(EXPENSE_ID, OWNER_ID);

            InOrder inOrder = inOrder(sharedWalletService, sharedExpenseRepository, outboxService);
            inOrder.verify(sharedWalletService).addBalanceToWallet(OWNER_ID, OLD_AMOUNT);
            inOrder.verify(sharedExpenseRepository).delete(existingExpense);
            inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
        }

        @Test
        void shouldThrowExceptionWhenExpenseDoesNotExist() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, OWNER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteExpense(EXPENSE_ID, OWNER_ID));

            verifyNoInteractions(sharedWalletService, sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryLookupFails() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, OWNER_ID))
                    .thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.deleteExpense(EXPENSE_ID, OWNER_ID));

            verifyNoInteractions(sharedWalletService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenWalletUpdateFails() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, OWNER_ID))
                    .thenReturn(Optional.of(existingExpense));
            when(existingExpense.getAmount()).thenReturn(OLD_AMOUNT);
            doThrow(new IllegalStateException("wallet failed")).when(sharedWalletService)
                    .addBalanceToWallet(OWNER_ID, OLD_AMOUNT);

            assertThrows(IllegalStateException.class, () -> service.deleteExpense(EXPENSE_ID, OWNER_ID));

            verify(sharedExpenseRepository, never()).delete(any(SharedExpense.class));
            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenParticipantsServiceFails() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, OWNER_ID))
                    .thenReturn(Optional.of(existingExpense));
            when(existingExpense.getAmount()).thenReturn(OLD_AMOUNT);
            when(sharedAccountParticipantsService.getParticipants(OWNER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("participants not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteExpense(EXPENSE_ID, OWNER_ID));

            verify(sharedExpenseRepository, never()).delete(any(SharedExpense.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenOutboxServiceFails() {
            stubSuccessfulDelete();
            doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                    .save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class, () -> service.deleteExpense(EXPENSE_ID, OWNER_ID));
        }
    }
}