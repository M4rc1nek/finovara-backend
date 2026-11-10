package com.finovara.financeservice.trash.service.deletion;

import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.trash.model.TrashEntityType;
import com.finovara.financeservice.trash.model.TrashItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class ExpensePermanentDeletionStrategy implements TrashPermanentDeletionStrategy {

    private final ExpenseRepository expenseRepository;

    @Override
    public boolean supports(TrashEntityType entityType) {
        return entityType == TrashEntityType.EXPENSE;
    }

    @Override
    public void delete(TrashItem item) {
        deleteAll(List.of(item));
    }

    @Override
    public void deleteAll(List<TrashItem> items) {
        List<Expense> expenses = expenseRepository.findAllByIdInAndDeletedAtIsNotNull(entityIds(items));
        if (expenses.size() != items.size()) {
            throw new IllegalStateException("One or more expenses for trash items no longer exist");
        }
        expenseRepository.deleteAll(expenses);
    }

    private List<Long> entityIds(List<TrashItem> items) {
        return items.stream().map(TrashItem::getEntityId).toList();
    }

}
