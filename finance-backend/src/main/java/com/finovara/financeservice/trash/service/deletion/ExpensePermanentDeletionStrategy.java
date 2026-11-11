package com.finovara.financeservice.trash.service.deletion;

import com.finovara.contracts.mainaccount.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.ExpenseActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ExpensePermanentDeletionStrategy implements TrashPermanentDeletionStrategy {

    private final ExpenseRepository expenseRepository;
    private final OutboxService outboxService;

    @Override
    public boolean supports(TrashEntityType entityType) {
        return entityType == TrashEntityType.EXPENSE;
    }

    @Override
    @Transactional
    public void delete(TrashItem item) {
        deleteAllExpenses(List.of(item));
    }

    @Override
    @Transactional
    public void deleteAll(List<TrashItem> items) {
        deleteAllExpenses(items);
    }

    private void deleteAllExpenses(List<TrashItem> items) {
        List<Expense> expenses = expenseRepository.findAllByIdInAndDeletedAtIsNotNull(entityIds(items));
        if (expenses.size() != items.size()) {
            throw new IllegalStateException("One or more expenses for trash items no longer exist");
        }
        expenses.forEach(expense -> outboxService.save("Expense", expense.getId().toString(),
                "trash.expense.activity", new ExpenseActivityEvent(expense.getUserId(),
                        ExpenseActivityType.PERMANENTLY_DELETED, expense.getAmount(), expense.getCategory(),
                        null, null, LocalDateTime.now())));
        expenseRepository.deleteAllInBatch(expenses);
    }

    private List<Long> entityIds(List<TrashItem> items) {
        return items.stream().map(TrashItem::getEntityId).toList();
    }
}
