package com.finovara.finovarabackend.expense.repository;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByIdAndUserAssignedId(Long expenseId, Long userId);

    List<Expense> findAllByUserAssignedId(Long userId);

    List<Expense> findAllByUserAssignedIdAndCreatedAtBetween(Long userId, LocalDate from, LocalDate to);


    // coalesce zwroci mi wydatki lub 0 jest wydatki sa null
    @Query("SELECT coalesce(sum(e.amount),0) from Expense e WHERE e.userAssigned.id = :userId AND e.createdAt = :date")
    BigDecimal sumExpenseForDay(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userAssigned.id = :userId AND e.createdAt >= :startDate AND e.createdAt <= :endDate")
    BigDecimal sumExpensesByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);


    @Query("SELECT e FROM Expense e WHERE e.userAssigned.id = :userId AND e.category = :category AND e.createdAt >= :startDate AND e.createdAt <= :endDate")
    List<Expense> findAllByUserAndCategoryAndDateRange(@Param("userId") Long userId, @Param("category") ExpenseCategory category,
                                                       @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

