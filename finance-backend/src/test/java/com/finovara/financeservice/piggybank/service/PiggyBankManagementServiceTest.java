package com.finovara.financeservice.piggybank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.mainaccount.activity.event.piggybank.PiggyBankEditActivityEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.util.model.PiggyBankGoalType;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.piggybank.dto.PiggyBankDto;
import com.finovara.financeservice.piggybank.goalplanner.service.GoalPlannerService;
import com.finovara.financeservice.piggybank.mapper.PiggyBankMapper;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.financeservice.settings.piggybank.model.PiggyBankSettings;
import com.finovara.financeservice.util.transaction.piggybank.manager.PiggyBankManagerService;
import com.finovara.financeservice.wallet.service.WalletService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PiggyBankManagementServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PIGGY_BANK_ID = 10L;
    private static final String AUTH_CODE = "123456";
    private static final String NAME = "Vacation";
    private static final String NEW_NAME = "New car";
    private static final BigDecimal GOAL_AMOUNT = new BigDecimal("1000.00");
    private static final BigDecimal NEW_GOAL_AMOUNT = new BigDecimal("2500.00");
    private static final PiggyBankGoalType GOAL_TYPE = PiggyBankGoalType.values()[0];
    private static final int MAX_PIGGY_BANKS = 3;

    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;
    @Mock
    private PiggyBankMapper piggyBankMapper;
    @Mock
    private PiggyBankSettingsFactory piggyBankSettingsFactory;
    @Mock
    private GoalPlannerService goalPlannerService;
    @Mock
    private AuthBackendClient authBackendClient;
    @Mock
    private AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;
    @Mock
    private WalletService walletService;
    @Mock
    private PiggyBankDto piggyBankDto;
    @Mock
    private PiggyBankDto otherPiggyBankDto;
    @Mock
    private PiggyBankSettings piggyBankSettings;
    @Mock
    private RecurringSettings recurringSettings;

    @InjectMocks
    private PiggyBankManagementService service;

    private PiggyBank piggyBank;

    @BeforeEach
    void setUp() {
        setField(service, "maxPiggyBanks", MAX_PIGGY_BANKS);
        piggyBank = PiggyBank.builder()
                .id(PIGGY_BANK_ID)
                .name(NAME)
                .amount(BigDecimal.ZERO)
                .goalAmount(GOAL_AMOUNT)
                .goalType(GOAL_TYPE)
                .userId(USER_ID)
                .build();
    }

    @Nested
    class AddPiggyBank {

        @Nested
        class WhenInputIsValid {

            @BeforeEach
            void setUp() {
                when(piggyBankDto.authorizationCode()).thenReturn(AUTH_CODE);
                when(piggyBankDto.name()).thenReturn(NAME);
                when(piggyBankDto.goalAmount()).thenReturn(GOAL_AMOUNT);
                when(piggyBankDto.goalType()).thenReturn(GOAL_TYPE);
                when(piggyBankRepository.findLowerCaseNamesByUserId(USER_ID)).thenReturn(List.of("first", "second"));
                when(piggyBankSettingsFactory.createDefaultPiggyBankSettings()).thenReturn(piggyBankSettings);
                when(piggyBankRepository.save(any(PiggyBank.class))).thenReturn(piggyBank);
            }

            @Test
            void shouldReturnSavedIdWhenInputIsValid() {
                Long result = service.addPiggyBank(piggyBankDto, USER_ID);

                assertEquals(PIGGY_BANK_ID, result);
            }

            @Test
            void shouldSavePiggyBankWithZeroAmountAndDefaultSettingsWhenInputIsValid() {
                ArgumentCaptor<PiggyBank> captor = ArgumentCaptor.forClass(PiggyBank.class);

                service.addPiggyBank(piggyBankDto, USER_ID);

                verify(piggyBankRepository).save(captor.capture());
                PiggyBank captured = captor.getValue();
                assertEquals(NAME, captured.getName());
                assertEquals(0, BigDecimal.ZERO.compareTo(captured.getAmount()));
                assertEquals(USER_ID, captured.getUserId());
                assertEquals(GOAL_AMOUNT, captured.getGoalAmount());
                assertEquals(GOAL_TYPE, captured.getGoalType());
                assertSame(piggyBankSettings, captured.getSettings());
                assertNotNull(captured.getCreatedAt());
            }

            @Test
            void shouldConfirmAuthorizationCodeWhenInputIsValid() {
                service.addPiggyBank(piggyBankDto, USER_ID);

                verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
                verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            }

            @Test
            void shouldLockWalletWhenInputIsValid() {
                service.addPiggyBank(piggyBankDto, USER_ID);

                verify(walletService).lockWalletForUpdate(USER_ID);
            }

            @Test
            void shouldPublishAddedEventWhenInputIsValid() {
                service.addPiggyBank(piggyBankDto, USER_ID);

                verify(outboxService).save(eq("PiggyBank"), eq(PIGGY_BANK_ID.toString()), eq("piggybank.lifecycle.changed"),
                        any(PiggyBankActivityEvent.class));
            }
        }

        @Test
        void shouldThrowExceptionWhenMaximumNumberOfPiggyBanksIsReached() {
            when(piggyBankDto.authorizationCode()).thenReturn(AUTH_CODE);
            when(piggyBankRepository.findLowerCaseNamesByUserId(USER_ID)).thenReturn(List.of("a", "b", "c"));

            assertThrows(InvalidInputException.class, () -> service.addPiggyBank(piggyBankDto, USER_ID));

            verify(piggyBankRepository, never()).save(any(PiggyBank.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenMaximumNumberOfPiggyBanksIsExceeded() {
            when(piggyBankDto.authorizationCode()).thenReturn(AUTH_CODE);
            when(piggyBankRepository.findLowerCaseNamesByUserId(USER_ID)).thenReturn(List.of("a", "b", "c", "d"));

            assertThrows(InvalidInputException.class, () -> service.addPiggyBank(piggyBankDto, USER_ID));

            verify(piggyBankRepository, never()).save(any(PiggyBank.class));
        }

        @Test
        void shouldThrowExceptionWhenNameAlreadyExists() {
            when(piggyBankDto.authorizationCode()).thenReturn(AUTH_CODE);
            when(piggyBankDto.name()).thenReturn(NAME);
            when(piggyBankRepository.findLowerCaseNamesByUserId(USER_ID)).thenReturn(List.of("vacation", "other"));

            assertThrows(EntityAlreadyExistsException.class, () -> service.addPiggyBank(piggyBankDto, USER_ID));

            verify(piggyBankRepository, never()).save(any(PiggyBank.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenNameAlreadyExistsWithDifferentLetterCase() {
            when(piggyBankDto.authorizationCode()).thenReturn(AUTH_CODE);
            when(piggyBankDto.name()).thenReturn("VACATION");
            when(piggyBankRepository.findLowerCaseNamesByUserId(USER_ID)).thenReturn(List.of("vacation"));

            assertThrows(EntityAlreadyExistsException.class, () -> service.addPiggyBank(piggyBankDto, USER_ID));

            verify(piggyBankRepository, never()).save(any(PiggyBank.class));
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            when(piggyBankDto.authorizationCode()).thenReturn(AUTH_CODE);
            doThrow(new IllegalStateException("Invalid code")).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.addPiggyBank(piggyBankDto, USER_ID));

            verifyNoInteractions(walletService, piggyBankRepository, outboxService);
        }
    }

    @Nested
    class EditPiggyBank {

        @Nested
        class WhenInputIsValid {

            @BeforeEach
            void setUp() {
                when(piggyBankDto.authorizationCode()).thenReturn(AUTH_CODE);
                when(piggyBankDto.name()).thenReturn(NEW_NAME);
                when(piggyBankDto.goalAmount()).thenReturn(NEW_GOAL_AMOUNT);
                when(piggyBankDto.goalType()).thenReturn(GOAL_TYPE);
                when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);
                when(piggyBankRepository.existsByNameIgnoreCaseExcludingId(USER_ID, NEW_NAME, PIGGY_BANK_ID)).thenReturn(false);
            }

            @Test
            void shouldReturnPiggyBankIdWhenInputIsValid() {
                Long result = service.editPiggyBank(USER_ID, piggyBankDto, PIGGY_BANK_ID);

                assertEquals(PIGGY_BANK_ID, result);
            }

            @Test
            void shouldUpdatePiggyBankFieldsWhenInputIsValid() {
                service.editPiggyBank(USER_ID, piggyBankDto, PIGGY_BANK_ID);

                assertEquals(NEW_NAME, piggyBank.getName());
                assertEquals(NEW_GOAL_AMOUNT, piggyBank.getGoalAmount());
                assertEquals(GOAL_TYPE, piggyBank.getGoalType());
            }

            @Test
            void shouldConfirmAuthorizationCodeWhenInputIsValid() {
                service.editPiggyBank(USER_ID, piggyBankDto, PIGGY_BANK_ID);

                verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
                verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
            }

            @Test
            void shouldCheckGoalCompletionWhenInputIsValid() {
                service.editPiggyBank(USER_ID, piggyBankDto, PIGGY_BANK_ID);

                verify(goalPlannerService).checkAndMarkGoalCompletion(piggyBank);
            }

            @Test
            void shouldPublishEditedEventWhenInputIsValid() {
                service.editPiggyBank(USER_ID, piggyBankDto, PIGGY_BANK_ID);

                verify(outboxService).save(eq("PiggyBank"), eq(PIGGY_BANK_ID.toString()), eq("piggybank.updated"),
                        any(PiggyBankEditActivityEvent.class));
            }
        }

        @Test
        void shouldThrowExceptionWhenNameAlreadyExistsForAnotherPiggyBank() {
            when(piggyBankDto.authorizationCode()).thenReturn(AUTH_CODE);
            when(piggyBankDto.name()).thenReturn(NEW_NAME);
            when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);
            when(piggyBankRepository.existsByNameIgnoreCaseExcludingId(USER_ID, NEW_NAME, PIGGY_BANK_ID)).thenReturn(true);

            assertThrows(EntityAlreadyExistsException.class, () -> service.editPiggyBank(USER_ID, piggyBankDto, PIGGY_BANK_ID));

            assertEquals(NAME, piggyBank.getName());
            verifyNoInteractions(goalPlannerService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankDoesNotExist() {
            when(piggyBankDto.authorizationCode()).thenReturn(AUTH_CODE);
            when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Piggy bank not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.editPiggyBank(USER_ID, piggyBankDto, PIGGY_BANK_ID));

            verifyNoInteractions(goalPlannerService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            when(piggyBankDto.authorizationCode()).thenReturn(AUTH_CODE);
            doThrow(new IllegalStateException("Invalid code")).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.editPiggyBank(USER_ID, piggyBankDto, PIGGY_BANK_ID));

            verifyNoInteractions(piggyBankManagerService, goalPlannerService, outboxService);
        }
    }

    @Nested
    class GetAllPiggyBanks {

        @Test
        void shouldReturnMappedDtosWhenUserHasPiggyBanks() {
            PiggyBank second = PiggyBank.builder()
                    .id(11L)
                    .name("Second")
                    .amount(new BigDecimal("100.00"))
                    .goalAmount(new BigDecimal("500.00"))
                    .goalType(GOAL_TYPE)
                    .userId(USER_ID)
                    .build();
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(piggyBank, second));
            when(piggyBankMapper.mapToPiggyBankDto(same(piggyBank), anyDouble(), anyBoolean())).thenReturn(piggyBankDto);
            when(piggyBankMapper.mapToPiggyBankDto(same(second), anyDouble(), anyBoolean())).thenReturn(otherPiggyBankDto);

            List<PiggyBankDto> result = service.getAllPiggyBanks(USER_ID);

            assertEquals(List.of(piggyBankDto, otherPiggyBankDto), result);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoPiggyBanks() {
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

            List<PiggyBankDto> result = service.getAllPiggyBanks(USER_ID);

            assertEquals(List.of(), result);
            verifyNoInteractions(piggyBankMapper);
        }
    }

    @Nested
    class DeletePiggyBank {

        @Test
        void shouldDeletePiggyBankWhenBalanceIsZero() {
            when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);
            when(recurringSettingsRepository.findByUserIdAndPiggyBankId(USER_ID, PIGGY_BANK_ID)).thenReturn(Optional.empty());

            service.deletePiggyBank(USER_ID, PIGGY_BANK_ID, AUTH_CODE);

            verify(piggyBankRepository).delete(piggyBank);
            verify(additionalAuthorizationCodeResolver).resolve(AUTH_CODE);
            verify(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());
        }

        @Test
        void shouldPublishDeletedEventWhenPiggyBankIsDeleted() {
            when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);
            when(recurringSettingsRepository.findByUserIdAndPiggyBankId(USER_ID, PIGGY_BANK_ID)).thenReturn(Optional.empty());

            service.deletePiggyBank(USER_ID, PIGGY_BANK_ID, AUTH_CODE);

            verify(outboxService).save(eq("PiggyBank"), eq(PIGGY_BANK_ID.toString()), eq("piggybank.lifecycle.changed"),
                    any(PiggyBankActivityEvent.class));
        }

        @Test
        void shouldDisableRecurringSettingsWhenTheyAreLinkedToPiggyBank() {
            when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);
            when(recurringSettingsRepository.findByUserIdAndPiggyBankId(USER_ID, PIGGY_BANK_ID)).thenReturn(Optional.of(recurringSettings));

            service.deletePiggyBank(USER_ID, PIGGY_BANK_ID, AUTH_CODE);

            verify(recurringSettings).setEnable(false);
            verify(recurringSettings).setPiggyBankId(null);
            verify(recurringSettings).setNextExecutionDate(null);
            verify(piggyBankRepository).delete(piggyBank);
        }

        @Test
        void shouldDeletePiggyBankWhenNoRecurringSettingsAreLinked() {
            when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);
            when(recurringSettingsRepository.findByUserIdAndPiggyBankId(USER_ID, PIGGY_BANK_ID)).thenReturn(Optional.empty());

            service.deletePiggyBank(USER_ID, PIGGY_BANK_ID, AUTH_CODE);

            verifyNoInteractions(recurringSettings);
            verify(piggyBankRepository).delete(piggyBank);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankHasBalance() {
            piggyBank.setAmount(new BigDecimal("50.00"));
            when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class, () -> service.deletePiggyBank(USER_ID, PIGGY_BANK_ID, AUTH_CODE));

            verify(piggyBankRepository, never()).delete(any(PiggyBank.class));
            verifyNoInteractions(recurringSettingsRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankHasMinimalPositiveBalance() {
            piggyBank.setAmount(new BigDecimal("0.01"));
            when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID)).thenReturn(piggyBank);

            assertThrows(InvalidInputException.class, () -> service.deletePiggyBank(USER_ID, PIGGY_BANK_ID, AUTH_CODE));

            verify(piggyBankRepository, never()).delete(any(PiggyBank.class));
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankDoesNotExist() {
            when(piggyBankManagerService.getPiggyBankForUpdateOrThrow(PIGGY_BANK_ID, USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Piggy bank not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deletePiggyBank(USER_ID, PIGGY_BANK_ID, AUTH_CODE));

            verify(piggyBankRepository, never()).delete(any(PiggyBank.class));
            verifyNoInteractions(recurringSettingsRepository, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenAuthorizationCodeIsInvalid() {
            doThrow(new IllegalStateException("Invalid code")).when(authBackendClient).confirmAuthorizationCode(eq(USER_ID), any());

            assertThrows(IllegalStateException.class, () -> service.deletePiggyBank(USER_ID, PIGGY_BANK_ID, AUTH_CODE));

            verifyNoInteractions(piggyBankManagerService, recurringSettingsRepository, outboxService);
            verify(piggyBankRepository, never()).delete(any(PiggyBank.class));
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteAllPiggyBanksWhenUserHasPiggyBanks() {
            List<PiggyBank> piggyBanks = List.of(piggyBank);
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(piggyBanks);

            service.deleteByUserId(USER_ID);

            verify(piggyBankRepository).deleteAll(piggyBanks);
        }

        @Test
        void shouldDeleteEmptyCollectionWhenUserHasNoPiggyBanks() {
            List<PiggyBank> piggyBanks = List.of();
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(piggyBanks);

            service.deleteByUserId(USER_ID);

            verify(piggyBankRepository).deleteAll(piggyBanks);
        }
    }
}