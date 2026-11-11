package com.finovara.financeservice.trash.service.deletion;

import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PiggyBankPermanentDeletionStrategy implements TrashPermanentDeletionStrategy {

    private final PiggyBankRepository piggyBankRepository;
    private final RecurringSettingsRepository recurringSettingsRepository;

    @Override
    public boolean supports(TrashEntityType entityType) {
        return entityType == TrashEntityType.PIGGY_BANK;
    }

    @Override
    @Transactional
    public void delete(TrashItem item) {
        deleteAllPiggyBanks(List.of(item));
    }

    @Override
    @Transactional
    public void deleteAll(List<TrashItem> items) {
        deleteAllPiggyBanks(items);
    }

    private void deleteAllPiggyBanks(List<TrashItem> items) {
        List<Long> piggyBankIds = entityIds(items);
        List<RecurringSettings> recurringSettings =
                recurringSettingsRepository.findAllByPiggyBankIdInForUpdate(piggyBankIds);
        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByIdInAndDeletedAtIsNotNull(piggyBankIds);
        if (piggyBanks.size() != items.size()) {
            throw new IllegalStateException("One or more piggy banks for trash items no longer exist");
        }

        unlinkRecurringSettings(recurringSettings);
        recurringSettingsRepository.saveAll(recurringSettings);
        piggyBankRepository.deleteAllInBatch(piggyBanks);
    }

    private void unlinkRecurringSettings(List<RecurringSettings> settings) {
        settings.forEach(setting -> {
            setting.setEnable(false);
            setting.setPiggyBankId(null);
            setting.setNextExecutionDate(null);
        });
    }

    private List<Long> entityIds(List<TrashItem> items) {
        return items.stream().map(TrashItem::getEntityId).toList();
    }
}
