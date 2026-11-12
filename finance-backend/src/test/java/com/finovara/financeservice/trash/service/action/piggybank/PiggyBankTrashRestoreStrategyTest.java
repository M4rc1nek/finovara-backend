package com.finovara.financeservice.trash.service.action.piggybank;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PiggyBankTrashRestoreStrategyTest {

    private static final Long PIGGY_BANK_ID = 10L;
    private static final Long USER_ID = 2L;

    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private PiggyBankTrashRestoreStrategy strategy;

    private PiggyBank piggyBank;
    private RecurringSettings recurringSettings;
    private TrashItem item;

    @BeforeEach
    void setUp() {
        piggyBank = PiggyBank.builder().id(PIGGY_BANK_ID).userId(USER_ID).name("Trip").build();
        recurringSettings = RecurringSettings.builder().id(20L).enable(true).build();
        item = TrashItem.builder()
                .entityType(TrashEntityType.PIGGY_BANK)
                .entityId(PIGGY_BANK_ID)
                .userId(USER_ID)
                .build();
        when(piggyBankRepository.findTrashedByIdAndUserIdForUpdate(PIGGY_BANK_ID, USER_ID))
                .thenReturn(Optional.of(piggyBank));
    }

    @Nested
    class Restore {
        @Test
        void shouldRestoreRecurringSettingWhenFlagIsTrue() {
            when(piggyBankRepository.existsByNameIgnoreCaseExcludingId(USER_ID, "Trip", PIGGY_BANK_ID))
                    .thenReturn(false);
            when(recurringSettingsRepository.findByUserIdAndPiggyBankIdForUpdate(USER_ID, PIGGY_BANK_ID))
                    .thenReturn(Optional.of(recurringSettings));
            item.setRestoreRecurringEnabled(true);

            strategy.restore(item);

            verify(piggyBankRepository).save(piggyBank);
            verify(recurringSettingsRepository).save(recurringSettings);
            assertTrue(recurringSettings.isEnable());
        }

        @Test
        void shouldKeepRecurringSettingDisabledWhenFlagIsFalse() {
            when(piggyBankRepository.existsByNameIgnoreCaseExcludingId(USER_ID, "Trip", PIGGY_BANK_ID))
                    .thenReturn(false);
            when(recurringSettingsRepository.findByUserIdAndPiggyBankIdForUpdate(USER_ID, PIGGY_BANK_ID))
                    .thenReturn(Optional.of(recurringSettings));
            item.setRestoreRecurringEnabled(false);

            strategy.restore(item);

            verify(recurringSettingsRepository).save(recurringSettings);
            assertFalse(recurringSettings.isEnable());
        }

        @Test
        void shouldKeepRecurringSettingDisabledWhenFlagIsNull() {
            when(piggyBankRepository.existsByNameIgnoreCaseExcludingId(USER_ID, "Trip", PIGGY_BANK_ID))
                    .thenReturn(false);
            when(recurringSettingsRepository.findByUserIdAndPiggyBankIdForUpdate(USER_ID, PIGGY_BANK_ID))
                    .thenReturn(Optional.of(recurringSettings));
            strategy.restore(item);

            verify(recurringSettingsRepository).save(recurringSettings);
            assertFalse(recurringSettings.isEnable());
        }

        @Test
        void shouldSkipRecurringRestoreWhenSettingsAreMissing() {
            when(piggyBankRepository.existsByNameIgnoreCaseExcludingId(USER_ID, "Trip", PIGGY_BANK_ID))
                    .thenReturn(false);
            when(recurringSettingsRepository.findByUserIdAndPiggyBankIdForUpdate(USER_ID, PIGGY_BANK_ID))
                    .thenReturn(Optional.empty());

            strategy.restore(item);

            verify(recurringSettingsRepository).findByUserIdAndPiggyBankIdForUpdate(USER_ID, PIGGY_BANK_ID);
            verify(recurringSettingsRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankIsMissingFromTrash() {
            when(piggyBankRepository.findTrashedByIdAndUserIdForUpdate(PIGGY_BANK_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> strategy.restore(item));
        }
    }
}
