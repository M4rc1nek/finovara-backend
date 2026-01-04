package com.finovara.finovarabackend.expense.repository;

import com.finovara.finovarabackend.expense.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByIdAndUserAssignedId(Long expenseId, Long userId);

    List<Expense> findAllByUserAssignedId(Long userId);


    List<Expense> findAllByUserAssignedIdAndCreatedAtBetween(Long userId, LocalDate from, LocalDate to);
}

