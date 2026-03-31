package com.finovara.finovarabackend.expense.repository;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.report.finances.chart.dto.DailyCashDto;
import com.finovara.finovarabackend.report.finances.highestexpense.dto.HighestExpenseDto;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT e From Expense e WHERE e.userAssigned.id = :userId AND e.createdAt BETWEEN :startDate AND :endDate")
    List<Expense> findAllByUserAssignedIdAndCreatedAtBetween(@Param("userId") Long userId, @Param("startDate") LocalDate from, @Param("endDate") LocalDate to);

    @Query("SELECT e From Expense e WHERE e.userAssigned.id = :userId AND e.createdAt BETWEEN :startDate AND :endDate AND e.category = :category")
    List<Expense> findAllByUserAssignedIdAndCreatedAtBetweenAndCategory(@Param("userId") Long userId, @Param("startDate") LocalDate from,
                                                                        @Param("endDate") LocalDate to, @Param("category") ExpenseCategory category);

    @Query("SELECT e FROM Expense e WHERE e.userAssigned.id = :userId ORDER BY e.id DESC")
    List<Expense> findFiveLastByUserAssignedId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.userAssigned.id = :userId")
    long countExpensesByUserAssignedId(Long userId);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.userAssigned.id = :userId AND e.createdAt BETWEEN :startDate AND :endDate")
    long countExpensesByUserAssignedIdAndCreatedAtBetween(Long userId, @Param("startDate") LocalDate from, @Param("endDate") LocalDate to);

    // coalesce zwroci mi wydatki lub 0 jest wydatki sa null
    @Query("SELECT coalesce(sum(e.amount),0) from Expense e WHERE e.userAssigned.id = :userId AND e.createdAt = :date")
    BigDecimal sumExpenseForDay(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userAssigned.id = :userId AND e.createdAt >= :startDate AND e.createdAt <= :endDate")
    BigDecimal sumExpensesByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT coalesce(sum(e.amount),0) FROM Expense e WHERE e.userAssigned.id = :userId")
    BigDecimal sumAllExpensesByUserAssignedId(Long userId);

    @Query("SELECT e FROM Expense e WHERE e.userAssigned.id = :userId AND e.category = :category AND e.createdAt >= :startDate AND e.createdAt <= :endDate")
    List<Expense> findAllByUserAndCategoryAndDateRange(@Param("userId") Long userId, @Param("category") ExpenseCategory category,
                                                       @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
             SELECT NEW com.finovara.finovarabackend.report.finances.highestexpense.dto.HighestExpenseDto(
             e.category,
             e.amount
            )
            FROM Expense e WHERE e.userAssigned.id = :userId AND e.createdAt BETWEEN :from AND :to ORDER BY e.amount DESC
            """)
    List<HighestExpenseDto> findHighestExpensesByUserAssignedIdAndPeriod(@Param("userId") Long userId, LocalDate from, LocalDate to, Pageable pageable);

    @Query("""
                SELECT new com.finovara.finovarabackend.report.finances.chart.dto.DateAmountDto(
                    e.createdAt,
                    SUM(e.amount)
                )
                FROM Expense e
                WHERE e.userAssigned.id = :userId
                GROUP BY e.createdAt
            """)
    List<DailyCashDto> sumExpensesGroupedByDate(Long userId);


    @Query("""
                SELECT new com.finovara.finovarabackend.report.finances.chart.dto.DateAmountDto(
                    e.createdAt,
                    CAST(AVG(e.amount) AS big_decimal)
                )
                FROM Expense e
                WHERE e.userAssigned.id = :userId
                GROUP BY e.createdAt
            """)
    List<DailyCashDto> avgExpensesGroupedByDate(Long userId);


}


