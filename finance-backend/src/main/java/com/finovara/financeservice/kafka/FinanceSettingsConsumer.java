package com.finovara.financeservice.kafka;

import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.event.user.UserAccountDeletedEvent;
import com.finovara.contracts.event.user.UserCreatedEvent;
import com.finovara.financeservice.settings.FinanceSettingsService;
import com.finovara.financeservice.settings.factory.FinanceSettingsFactory;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FinanceSettingsConsumer {

    private final List<UserDataDeletable> deletableServices;
    private final FinanceSettingsFactory financeSettingsFactory;
    private final FinanceSettingsService financeSettingsService;
    private final ExpenseSettingsRepository expenseSettingsRepository;

    @KafkaListener(topics = "user.created", groupId = "finance.default-settings.expense")
    public void createDefaultExpenseSettings(UserCreatedEvent event) {
        financeSettingsFactory.createDefaultExpenseSettingsIfNotExist(event.userId());
    }

    @KafkaListener(topics = "user.created", groupId = "finance.default-settings.recurring")
    public void createDefaultRecurringSettings(UserCreatedEvent event) {
        financeSettingsFactory.createDefaultRecurringSettingsIfNotExist(event.userId());
    }

    @KafkaListener(topics = "user-account.deleted", groupId = "finance.delete-recurring-settings")
    public void deleteRecurringSettings(UserAccountDeletedEvent event) {
        financeSettingsService.deleteRecurringSettings(event.userId());
    }

    @KafkaListener(topics = "user-account.deleted", groupId = "finance.delete-expense-settings")
    public void deleteExpenseSettings(UserAccountDeletedEvent event) {
        financeSettingsService.deleteExpenseSettings(event.userId());
    }

    @KafkaListener(topics = "user-account.deleted")
    public void handleAccountDeleted(UserAccountDeletedEvent event) {
        deletableServices.forEach(service -> service.deleteByUserId(event.userId()));
    }

}
