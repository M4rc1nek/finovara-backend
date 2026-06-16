package com.finovara.financeservice.settings.finances.expense.repository;

import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpenseSettingsRepository extends JpaRepository<ExpenseSettings, Long> {

    ExpenseSettings findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
