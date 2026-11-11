package com.finovara.financeservice.trash.service.deletion;

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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PiggyBankPermanentDeletionStrategyTest {

    private static final Long PIGGY_BANK_ID = 10L;
    private static final Long RECURRING_SETTINGS_ID = 20L;

    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    @InjectMocks
    private PiggyBankPermanentDeletionStrategy strategy;

    private TrashItem item;
    private PiggyBank piggyBank;
    private RecurringSettings recurringSettings;

    @BeforeEach
    void setUp() {
        item = TrashItem.builder().entityType(TrashEntityType.PIGGY_BANK).entityId(PIGGY_BANK_ID).build();
        piggyBank = PiggyBank.builder().id(PIGGY_BANK_ID).deletedAt(LocalDateTime.now()).build();
        recurringSettings = RecurringSettings.builder()
                .id(RECURRING_SETTINGS_ID)
                .piggyBankId(PIGGY_BANK_ID)
                .enable(true)
                .build();
        when(recurringSettingsRepository.findAllByPiggyBankIdInForUpdate(List.of(PIGGY_BANK_ID)))
                .thenReturn(List.of(recurringSettings));
        when(piggyBankRepository.findAllByIdInAndDeletedAtIsNotNull(List.of(PIGGY_BANK_ID)))
                .thenReturn(List.of(piggyBank));
    }

    @Nested
    class DeleteAll {
        @Test
        void shouldUnlinkRecurringSettingsAndDeleteLoadedPiggyBanks() {
            strategy.deleteAll(List.of(item));

            InOrder inOrder = inOrder(recurringSettingsRepository, piggyBankRepository);
            inOrder.verify(recurringSettingsRepository).findAllByPiggyBankIdInForUpdate(List.of(PIGGY_BANK_ID));
            inOrder.verify(piggyBankRepository).findAllByIdInAndDeletedAtIsNotNull(List.of(PIGGY_BANK_ID));
            inOrder.verify(recurringSettingsRepository).saveAll(List.of(recurringSettings));
            inOrder.verify(piggyBankRepository).deleteAllInBatch(List.of(piggyBank));
            assertFalse(recurringSettings.isEnable());
        }

        @Test
        void shouldThrowExceptionWithoutDeletingWhenPiggyBankIsMissing() {
            when(piggyBankRepository.findAllByIdInAndDeletedAtIsNotNull(List.of(PIGGY_BANK_ID)))
                    .thenReturn(List.of());

            assertThrows(IllegalStateException.class, () -> strategy.deleteAll(List.of(item)));

            verify(piggyBankRepository, never()).deleteAllInBatch(List.of(piggyBank));
            verify(recurringSettingsRepository, never()).saveAll(List.of(recurringSettings));
        }
    }
}
