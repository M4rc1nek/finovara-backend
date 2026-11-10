package com.finovara.financeservice.trash.service.action.piggybank;

import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.mainaccount.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.PiggyBankActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import com.finovara.financeservice.trash.service.TrashRestoreStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PiggyBankTrashRestoreStrategy implements TrashRestoreStrategy {

    private final PiggyBankRepository piggyBankRepository;
    private final RecurringSettingsRepository recurringSettingsRepository;
    private final OutboxService outboxService;

    @Override
    public boolean supports(TrashEntityType entityType) {
        return entityType == TrashEntityType.PIGGY_BANK;
    }

    @Override
    public void restore(TrashItem item) {
        Optional<RecurringSettings> recurringSettings = getRecurringSettings(item);
        PiggyBank piggyBank = getPiggyBank(item);
        validateNameIsAvailable(item, piggyBank);
        restorePiggyBank(piggyBank);
        restoreRecurringSetting(item, recurringSettings);
        publishPiggyBankRestoredEvent(item, piggyBank);
    }

    private Optional<RecurringSettings> getRecurringSettings(TrashItem item) {
        return recurringSettingsRepository.findByUserIdAndPiggyBankIdForUpdate(item.getUserId(), item.getEntityId());
    }

    private PiggyBank getPiggyBank(TrashItem item) {
        return piggyBankRepository.findTrashedByIdAndUserIdForUpdate(item.getEntityId(), item.getUserId())
                .orElseThrow(() -> new RequestedEntityNotFoundException("Piggy bank not found in trash"));
    }

    private void validateNameIsAvailable(TrashItem item, PiggyBank piggyBank) {
        if (piggyBankRepository.existsByNameIgnoreCaseExcludingId(item.getUserId(), piggyBank.getName(), piggyBank.getId())) {
            throw new EntityAlreadyExistsException("This piggy bank name already exists");
        }
    }

    private void restorePiggyBank(PiggyBank piggyBank) {
        piggyBank.setDeletedAt(null);
        piggyBankRepository.save(piggyBank);
    }

    private void restoreRecurringSetting(TrashItem item, Optional<RecurringSettings> recurringSettings) {
        recurringSettings.ifPresent(settings -> restoreRecurringSetting(item, settings));
    }

    private void restoreRecurringSetting(TrashItem item, RecurringSettings settings) {
        settings.setEnable(Boolean.TRUE.equals(item.getRestoreRecurringEnabled()));
        recurringSettingsRepository.save(settings);
    }

    private void publishPiggyBankRestoredEvent(TrashItem item, PiggyBank piggyBank) {
        outboxService.save("PiggyBank", piggyBank.getId().toString(), "piggybank.lifecycle.changed",
                new PiggyBankActivityEvent(item.getUserId(), PiggyBankActivityType.ADDED_PIGGY_BANK,
                        piggyBank.getName(), piggyBank.getGoalType(), piggyBank.getGoalAmount(),
                        null, null, LocalDateTime.now()));
    }
}
