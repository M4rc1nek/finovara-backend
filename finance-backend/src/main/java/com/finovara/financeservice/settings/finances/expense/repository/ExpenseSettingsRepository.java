package com.finovara.financeservice.settings.finances.expense.repository;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseSettingsRepository extends JpaRepository<ExpenseSettings, Long> {

    ExpenseSettings findByUserId(Long userId);

    default ExpenseSettings findByUserIdOrThrow(Long userId) {
        ExpenseSettings settings = findByUserId(userId);
        if (settings == null) {
            throw new RequestedEntityNotFoundException("Expense settings not found for user");
        }
        return settings;
    }
}
