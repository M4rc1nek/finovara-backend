package com.finovara.financeservice.sharedaccount.expense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseDto;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseRequest;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseResponse;
import com.finovara.financeservice.sharedaccount.expense.mapper.SharedExpenseMapper;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.sharedaccount.limit.service.SharedLimitSpendingService;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.dto.ExpenseAnalysisMode;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.service.ExpenseAnalysisService;
import com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.service.LargeExpenseNotificationService;
import com.finovara.financeservice.sharedaccount.settings.expense.spendcontrol.service.SpendControlService;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.sharedaccount.SharedUsernameResolver;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
class SharedExpenseServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 3L;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long EXPENSE_ID = 5L;
    private static final Long LIMIT_ID = 7L;
    private static final String USERNAME = "john";
    private static final String OTHER_USERNAME = "anna";
    private static final String OUTBOX_AGGREGATE = "SharedAccountExpense";
    private static final String OUTBOX_TOPIC = "shared-account.activity";
    private static final int PAGE_SIZE = 10;
    private static final BigDecimal AMOUNT = BigDecimal.TEN;
    private static final BigDecimal LIMIT_AMOUNT = BigDecimal.valueOf(100);
    private static final ExpenseCategory CATEGORY = ExpenseCategory.values()[0];
    private static final ExpenseCategory OTHER_CATEGORY = ExpenseCategory.values()[1];
    private static final PeriodType PERIOD = PeriodType.values()[0];

    @Mock
    private SharedExpenseRepository sharedExpenseRepository;

    @Mock
    private SharedWalletService sharedWalletService;

    @Mock
    private SharedLimitRepository sharedLimitRepository;

    @Mock
    private SharedLimitSpendingService sharedLimitSpendingService;

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
    private SharedUsernameResolver usernameResolver;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SharedExpenseRequest request;

    @Mock
    private SharedExpenseDto expenseDto;

    @Mock
    private SharedExpenseDto mappedDto;

    @Mock
    private SharedExpenseDto otherMappedDto;

    @Mock
    private SharedAccountParticipantsResponse participants;

    @Captor
    private ArgumentCaptor<SharedExpense> expenseCaptor;

    @InjectMocks
    private SharedExpenseService service;

    private SharedLimit generalLimit;
    private SharedLimit categoryLimit;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", PAGE_SIZE);
        generalLimit = SharedLimit.builder().id(LIMIT_ID).periodType(PERIOD).category(null).amount(LIMIT_AMOUNT).build();
        categoryLimit = SharedLimit.builder().id(LIMIT_ID).periodType(PERIOD).category(CATEGORY).amount(LIMIT_AMOUNT).build();
    }

    @Nested
    class AddExpense {

        @BeforeEach
        void setUp() {
            when(request.sharedExpenseDto()).thenReturn(expenseDto);
            when(expenseDto.category()).thenReturn(CATEGORY);
        }

        @Nested
        class AmountValidation {

            @Test
            void shouldThrowExceptionWhenAmountIsNull() {
                assertThrows(InvalidInputException.class, () -> service.addExpense(request, USER_ID));
                verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedExpenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenAmountIsBelowOne() {
                when(expenseDto.amount()).thenReturn(new BigDecimal("0.99"));

                assertThrows(InvalidInputException.class, () -> service.addExpense(request, USER_ID));
                verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedExpenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenAmountIsNegative() {
                when(expenseDto.amount()).thenReturn(BigDecimal.valueOf(-3));

                assertThrows(InvalidInputException.class, () -> service.addExpense(request, USER_ID));
                verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedExpenseRepository, outboxService);
            }
        }

        @Nested
        class ValidAmount {

            @BeforeEach
            void setUp() {
                when(expenseDto.amount()).thenReturn(AMOUNT);
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
                when(usernameResolver.resolve(List.of(USER_ID))).thenReturn(Map.of(USER_ID, USERNAME));
            }

            @Nested
            class Persisted {

                private SharedExpense savedExpense;

                @BeforeEach
                void setUp() {
                    when(participants.ownerId()).thenReturn(OWNER_ID);
                    when(participants.memberId()).thenReturn(MEMBER_ID);
                    savedExpense = SharedExpense.builder().id(EXPENSE_ID).amount(AMOUNT).category(CATEGORY).createdByUserId(USER_ID).build();
                    when(sharedExpenseRepository.save(any(SharedExpense.class))).thenReturn(savedExpense);
                }

                @Test
                void shouldReturnResponseWithIdAndUsernameWhenExpenseAdded() {
                    when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

                    SharedExpenseResponse result = service.addExpense(request, USER_ID);

                    assertEquals(new SharedExpenseResponse(EXPENSE_ID, USER_ID, USERNAME), result);
                }

                @Test
                void shouldSaveExpenseWithParticipantsAndCreatorWhenExpenseAdded() {
                    when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

                    service.addExpense(request, USER_ID);

                    verify(sharedExpenseRepository).save(expenseCaptor.capture());
                    assertEquals(AMOUNT, expenseCaptor.getValue().getAmount());
                    assertEquals(CATEGORY, expenseCaptor.getValue().getCategory());
                    assertEquals(OWNER_ID, expenseCaptor.getValue().getOwnerId());
                    assertEquals(MEMBER_ID, expenseCaptor.getValue().getMemberId());
                    assertEquals(USER_ID, expenseCaptor.getValue().getCreatedByUserId());
                    assertEquals(LocalDate.now(), expenseCaptor.getValue().getCreatedAt());
                }

                @Test
                void shouldApplyAllSideEffectsWhenExpenseAdded() {
                    when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

                    service.addExpense(request, USER_ID);

                    verify(expenseAnalysisService).handleExpenseAnalysis(eq(USER_ID), any(), eq(AMOUNT), eq(ExpenseAnalysisMode.ADD));
                    verify(spendControlService).handleSpendControl(USER_ID, AMOUNT);
                    verify(sharedWalletService).lockWalletForUpdate(USER_ID);
                    verify(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);
                    verify(largeExpenseNotificationService).handleLargeNotification(USER_ID, savedExpense);
                    verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(EXPENSE_ID.toString()), eq(OUTBOX_TOPIC), any(SharedAccountActivityLogEvent.class));
                }

                @Test
                void shouldLockWalletBeforeSpendControlAndRemovingBalance() {
                    when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

                    service.addExpense(request, USER_ID);

                    InOrder inOrder = inOrder(expenseAnalysisService, sharedWalletService, spendControlService, sharedExpenseRepository);
                    inOrder.verify(expenseAnalysisService).handleExpenseAnalysis(eq(USER_ID), any(), eq(AMOUNT), eq(ExpenseAnalysisMode.ADD));
                    inOrder.verify(sharedWalletService).lockWalletForUpdate(USER_ID);
                    inOrder.verify(spendControlService).handleSpendControl(USER_ID, AMOUNT);
                    inOrder.verify(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);
                    inOrder.verify(sharedExpenseRepository).save(any(SharedExpense.class));
                }

                @Test
                void shouldNotCalculateSpentWhenNoLimitsDefined() {
                    when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

                    service.addExpense(request, USER_ID);

                    verifyNoInteractions(sharedLimitSpendingService);
                }

                @Test
                void shouldSaveExpenseWhenTotalEqualsLimitAmount() {
                    List<SharedLimit> limits = List.of(generalLimit);
                    when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(limits);
                    when(sharedLimitSpendingService.calculateSpent(eq(participants), eq(limits), any(LocalDate.class)))
                            .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(90)));

                    SharedExpenseResponse result = service.addExpense(request, USER_ID);

                    assertEquals(EXPENSE_ID, result.hashCode() == 0 ? null : EXPENSE_ID);
                    verify(sharedExpenseRepository).save(any(SharedExpense.class));
                }

                @Test
                void shouldTreatMissingSpentAsZeroWhenLimitHasNoSpentEntry() {
                    List<SharedLimit> limits = List.of(generalLimit);
                    when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(limits);
                    when(sharedLimitSpendingService.calculateSpent(eq(participants), eq(limits), any(LocalDate.class)))
                            .thenReturn(Map.of());

                    service.addExpense(request, USER_ID);

                    verify(sharedExpenseRepository).save(any(SharedExpense.class));
                }

                @Test
                void shouldIgnoreLimitWhenLimitCategoryDiffersFromExpenseCategory() {
                    SharedLimit otherCategoryLimit = SharedLimit.builder().id(LIMIT_ID).periodType(PERIOD).category(OTHER_CATEGORY).amount(LIMIT_AMOUNT).build();
                    List<SharedLimit> limits = List.of(otherCategoryLimit);
                    when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(limits);
                    when(sharedLimitSpendingService.calculateSpent(eq(participants), eq(limits), any(LocalDate.class)))
                            .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(1000)));

                    service.addExpense(request, USER_ID);

                    verify(sharedExpenseRepository).save(any(SharedExpense.class));
                }
            }

            @Test
            void shouldThrowExceptionWhenGeneralLimitExceeded() {
                List<SharedLimit> limits = List.of(generalLimit);
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(limits);
                when(sharedLimitSpendingService.calculateSpent(eq(participants), eq(limits), any(LocalDate.class)))
                        .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(91)));

                assertThrows(MissingRequirementException.class, () -> service.addExpense(request, USER_ID));
                verify(sharedExpenseRepository, never()).save(any(SharedExpense.class));
                verify(sharedWalletService, never()).removeBalanceFromWallet(any(), any());
                verifyNoInteractions(outboxService, largeExpenseNotificationService);
            }

            @Test
            void shouldThrowExceptionWhenCategoryLimitExceeded() {
                List<SharedLimit> limits = List.of(categoryLimit);
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(limits);
                when(sharedLimitSpendingService.calculateSpent(eq(participants), eq(limits), any(LocalDate.class)))
                        .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(100)));

                assertThrows(MissingRequirementException.class, () -> service.addExpense(request, USER_ID));
                verify(sharedExpenseRepository, never()).save(any(SharedExpense.class));
                verifyNoInteractions(outboxService, largeExpenseNotificationService);
            }

            @Test
            void shouldThrowExceptionWhenExpenseAnalysisRejectsExpense() {
                doThrow(new IllegalStateException()).when(expenseAnalysisService)
                        .handleExpenseAnalysis(eq(USER_ID), any(), eq(AMOUNT), eq(ExpenseAnalysisMode.ADD));

                assertThrows(IllegalStateException.class, () -> service.addExpense(request, USER_ID));
                verifyNoInteractions(sharedWalletService, sharedExpenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenSpendControlRejectsExpense() {
                doThrow(new IllegalStateException()).when(spendControlService).handleSpendControl(USER_ID, AMOUNT);

                assertThrows(IllegalStateException.class, () -> service.addExpense(request, USER_ID));
                verify(sharedWalletService, never()).removeBalanceFromWallet(any(), any());
                verifyNoInteractions(sharedExpenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenWalletBalanceIsInsufficient() {
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of());
                doThrow(new IllegalStateException()).when(sharedWalletService).removeBalanceFromWallet(USER_ID, AMOUNT);

                assertThrows(IllegalStateException.class, () -> service.addExpense(request, USER_ID));
                verifyNoInteractions(sharedExpenseRepository, outboxService, largeExpenseNotificationService);
            }
        }
    }

    @Nested
    class EditExpense {

        @BeforeEach
        void setUp() {
            when(request.sharedExpenseDto()).thenReturn(expenseDto);
            when(expenseDto.category()).thenReturn(CATEGORY);
        }

        @Nested
        class AmountValidation {

            @Test
            void shouldThrowExceptionWhenAmountIsNull() {
                assertThrows(InvalidInputException.class, () -> service.editExpense(request, USER_ID, EXPENSE_ID));
                verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedExpenseRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenAmountIsBelowOne() {
                when(expenseDto.amount()).thenReturn(new BigDecimal("0.5"));

                assertThrows(InvalidInputException.class, () -> service.editExpense(request, USER_ID, EXPENSE_ID));
                verifyNoInteractions(sharedAccountParticipantsService, sharedWalletService, sharedExpenseRepository, outboxService);
            }
        }

        @Nested
        class ValidAmount {

            private SharedExpense existing;

            @BeforeEach
            void setUp() {
                when(expenseDto.amount()).thenReturn(AMOUNT);
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
                existing = SharedExpense.builder()
                        .id(EXPENSE_ID)
                        .amount(BigDecimal.valueOf(5))
                        .category(CATEGORY)
                        .createdAt(LocalDate.now())
                        .createdByUserId(USER_ID)
                        .build();
            }

            @Nested
            class Persisted {

                @BeforeEach
                void setUp() {
                    when(participants.ownerId()).thenReturn(OWNER_ID);
                    when(participants.memberId()).thenReturn(MEMBER_ID);
                    when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, USER_ID)).thenReturn(Optional.of(existing));
                }

                @Test
                void shouldUpdateExpenseAndReturnIdWhenAmountIncreased() {
                    when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

                    Long result = service.editExpense(request, USER_ID, EXPENSE_ID);

                    assertEquals(EXPENSE_ID, result);
                    assertEquals(AMOUNT, existing.getAmount());
                    assertEquals(CATEGORY, existing.getCategory());
                    verify(sharedWalletService).removeBalanceFromWallet(USER_ID, BigDecimal.valueOf(5));
                    verify(sharedWalletService, never()).addBalanceToWallet(any(), any());
                }

                @Test
                void shouldAddBalanceToWalletWhenAmountDecreased() {
                    existing.setAmount(BigDecimal.valueOf(25));
                    when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

                    service.editExpense(request, USER_ID, EXPENSE_ID);

                    verify(sharedWalletService).addBalanceToWallet(USER_ID, BigDecimal.valueOf(15));
                    verify(sharedWalletService, never()).removeBalanceFromWallet(any(), any());
                }

                @Test
                void shouldNotChangeWalletBalanceWhenAmountUnchanged() {
                    existing.setAmount(AMOUNT);
                    when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

                    service.editExpense(request, USER_ID, EXPENSE_ID);

                    verify(sharedWalletService, never()).addBalanceToWallet(any(), any());
                    verify(sharedWalletService, never()).removeBalanceFromWallet(any(), any());
                }

                @Test
                void shouldAnalyzeExpenseAndLockWalletAndLogActivityWhenExpenseEdited() {
                    when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

                    service.editExpense(request, USER_ID, EXPENSE_ID);

                    verify(expenseAnalysisService).handleExpenseAnalysis(eq(USER_ID), any(), eq(AMOUNT), eq(ExpenseAnalysisMode.EDIT));
                    verify(sharedWalletService).lockWalletForUpdate(USER_ID);
                    verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(EXPENSE_ID.toString()), eq(OUTBOX_TOPIC), any(SharedAccountActivityLogEvent.class));
                }

                @Test
                void shouldSucceedWhenTotalAfterReplacingOldAmountEqualsLimit() {
                    List<SharedLimit> limits = List.of(generalLimit);
                    when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(limits);
                    when(sharedLimitSpendingService.calculateSpent(eq(participants), eq(limits), any(LocalDate.class)))
                            .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(95)));

                    Long result = service.editExpense(request, USER_ID, EXPENSE_ID);

                    assertEquals(EXPENSE_ID, result);
                    assertEquals(AMOUNT, existing.getAmount());
                }

                @Test
                void shouldIgnoreLimitWhenExpenseWasCreatedBeforePeriodStart() {
                    existing.setCreatedAt(PERIOD.getStartDate(LocalDate.now()).minusDays(1));
                    List<SharedLimit> limits = List.of(generalLimit);
                    when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(limits);
                    when(sharedLimitSpendingService.calculateSpent(eq(participants), eq(limits), any(LocalDate.class)))
                            .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(1000)));

                    Long result = service.editExpense(request, USER_ID, EXPENSE_ID);

                    assertEquals(EXPENSE_ID, result);
                    assertEquals(AMOUNT, existing.getAmount());
                }
            }

            @Test
            void shouldThrowExceptionWhenGeneralLimitExceededAfterReplacingOldAmount() {
                List<SharedLimit> limits = List.of(generalLimit);
                when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, USER_ID)).thenReturn(Optional.of(existing));
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(limits);
                when(sharedLimitSpendingService.calculateSpent(eq(participants), eq(limits), any(LocalDate.class)))
                        .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(100)));

                assertThrows(MissingRequirementException.class, () -> service.editExpense(request, USER_ID, EXPENSE_ID));
                assertEquals(BigDecimal.valueOf(5), existing.getAmount());
                verify(sharedWalletService, never()).removeBalanceFromWallet(any(), any());
                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldNotSubtractOldAmountWhenOldCategoryDoesNotMatchLimit() {
                existing.setCategory(OTHER_CATEGORY);
                List<SharedLimit> limits = List.of(categoryLimit);
                when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, USER_ID)).thenReturn(Optional.of(existing));
                when(sharedLimitRepository.findAllByUserId(USER_ID)).thenReturn(limits);
                when(sharedLimitSpendingService.calculateSpent(eq(participants), eq(limits), any(LocalDate.class)))
                        .thenReturn(Map.of(LIMIT_ID, BigDecimal.valueOf(95)));

                assertThrows(MissingRequirementException.class, () -> service.editExpense(request, USER_ID, EXPENSE_ID));
                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenExpenseNotFound() {
                when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, USER_ID)).thenReturn(Optional.empty());

                assertThrows(RequestedEntityNotFoundException.class, () -> service.editExpense(request, USER_ID, EXPENSE_ID));
                verify(sharedWalletService, never()).removeBalanceFromWallet(any(), any());
                verify(sharedWalletService, never()).addBalanceToWallet(any(), any());
                verifyNoInteractions(sharedLimitRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenExpenseAnalysisRejectsExpense() {
                doThrow(new IllegalStateException()).when(expenseAnalysisService)
                        .handleExpenseAnalysis(eq(USER_ID), any(), eq(AMOUNT), eq(ExpenseAnalysisMode.EDIT));

                assertThrows(IllegalStateException.class, () -> service.editExpense(request, USER_ID, EXPENSE_ID));
                verifyNoInteractions(sharedWalletService, sharedExpenseRepository, outboxService);
            }
        }
    }

    @Nested
    class GetExpense {

        @Test
        void shouldReturnMappedExpensesWithUsernamesWhenExpensesExist() {
            SharedExpense first = SharedExpense.builder().id(EXPENSE_ID).amount(AMOUNT).category(CATEGORY).createdByUserId(USER_ID).build();
            SharedExpense second = SharedExpense.builder().id(6L).amount(AMOUNT).category(CATEGORY).createdByUserId(OTHER_USER_ID).build();
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(USER_ID, PageRequest.of(2, PAGE_SIZE))).thenReturn(List.of(first, second));
            when(usernameResolver.resolve(Set.of(USER_ID, OTHER_USER_ID))).thenReturn(Map.of(USER_ID, USERNAME, OTHER_USER_ID, OTHER_USERNAME));
            when(sharedExpenseMapper.mapToDto(first, USERNAME)).thenReturn(mappedDto);
            when(sharedExpenseMapper.mapToDto(second, OTHER_USERNAME)).thenReturn(otherMappedDto);

            List<SharedExpenseDto> result = service.getExpense(USER_ID, 2);

            assertEquals(2, result.size());
            assertSame(mappedDto, result.get(0));
            assertSame(otherMappedDto, result.get(1));
        }

        @Test
        void shouldPassNullUsernameToMapperWhenUsernameNotResolved() {
            SharedExpense expense = SharedExpense.builder().id(EXPENSE_ID).amount(AMOUNT).category(CATEGORY).createdByUserId(USER_ID).build();
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(USER_ID, PageRequest.of(0, PAGE_SIZE))).thenReturn(List.of(expense));
            when(usernameResolver.resolve(Set.of(USER_ID))).thenReturn(Map.of());
            when(sharedExpenseMapper.mapToDto(expense, null)).thenReturn(mappedDto);

            List<SharedExpenseDto> result = service.getExpense(USER_ID, 0);

            assertSame(mappedDto, result.get(0));
        }

        @Test
        void shouldReturnEmptyListWhenNoExpensesExist() {
            when(sharedExpenseRepository.findAllByOwnerIdOrMemberId(USER_ID, PageRequest.of(0, PAGE_SIZE))).thenReturn(List.of());

            List<SharedExpenseDto> result = service.getExpense(USER_ID, 0);

            assertTrue(result.isEmpty());
            verifyNoInteractions(sharedExpenseMapper);
        }
    }

    @Nested
    class DeleteExpense {

        @Test
        void shouldDeleteExpenseAndRestoreBalanceWhenExpenseExists() {
            SharedExpense expense = SharedExpense.builder().id(EXPENSE_ID).amount(AMOUNT).category(CATEGORY).createdByUserId(USER_ID).build();
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, USER_ID)).thenReturn(Optional.of(expense));
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);

            service.deleteExpense(EXPENSE_ID, USER_ID);

            InOrder inOrder = inOrder(sharedWalletService, sharedExpenseRepository, outboxService);
            inOrder.verify(sharedWalletService).lockWalletForUpdate(USER_ID);
            inOrder.verify(sharedWalletService).addBalanceToWallet(USER_ID, AMOUNT);
            inOrder.verify(sharedExpenseRepository).delete(expense);
            inOrder.verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(EXPENSE_ID.toString()), eq(OUTBOX_TOPIC), any(SharedAccountActivityLogEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenExpenseNotFound() {
            when(sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(EXPENSE_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteExpense(EXPENSE_ID, USER_ID));
            verify(sharedWalletService, never()).addBalanceToWallet(any(), any());
            verify(sharedExpenseRepository, never()).delete(any(SharedExpense.class));
            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }
    }
}