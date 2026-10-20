package com.finovara.financeservice.settings.finances.expense.repository;

import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpenseSettingsRepository extends JpaRepository<ExpenseSettings, Long> {

    @Query("SELECT es FROM ExpenseSettings es WHERE es.userId = :userId")
    Optional<ExpenseSettings> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
