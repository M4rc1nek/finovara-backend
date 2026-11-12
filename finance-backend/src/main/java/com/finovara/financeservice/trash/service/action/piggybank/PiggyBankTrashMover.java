package com.finovara.financeservice.trash.service.action.piggybank;

import com.finovara.contracts.mainaccount.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.PiggyBankActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.service.TrashService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PiggyBankTrashMover {

    private final PiggyBankRepository piggyBankRepository;
    private final RecurringSettingsRepository recurringSettingsRepository;
    private final OutboxService outboxService;
    private final TrashService trashService;

    @Transactional
    public void move(PiggyBank piggyBank, Long userId) {
        boolean recurringWasEnabled = pauseRecurringSettings(userId, piggyBank.getId());
        LocalDateTime deletedAt = LocalDateTime.now();
        piggyBank.setDeletedAt(deletedAt);
        piggyBankRepository.save(piggyBank);
        publishAddedToTrashEvent(piggyBank, userId, deletedAt);
        trashService.add(TrashEntityType.PIGGY_BANK, piggyBank.getId(), userId, deletedAt,
                recurringWasEnabled, false);
    }

    private boolean pauseRecurringSettings(Long userId, Long piggyBankId) {
        Optional<RecurringSettings> recurringSettings = recurringSettingsRepository.findByUserIdAndPiggyBankIdForUpdate(userId, piggyBankId);
        if (recurringSettings.isEmpty()) {
            return false;
        }

        RecurringSettings settings = recurringSettings.get();
        boolean wasEnabled = settings.isEnable();
        settings.setEnable(false);
        recurringSettingsRepository.save(settings);
        return wasEnabled;
    }

    private void publishAddedToTrashEvent(PiggyBank piggyBank, Long userId, LocalDateTime deletedAt) {
        outboxService.save("PiggyBank", piggyBank.getId().toString(), "trash.piggybank.activity",
                new PiggyBankActivityEvent(userId, PiggyBankActivityType.ADDED_TO_TRASH,
                        piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(),
                        null, null, deletedAt));
    }
}
