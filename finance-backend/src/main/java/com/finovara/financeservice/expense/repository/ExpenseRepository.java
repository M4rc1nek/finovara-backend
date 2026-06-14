package com.finovara.authbackend.expense.repository;

import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.authbackend.expense.model.Expense;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
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

    Optional<Expense> findByIdAndUserId(Long expenseId, Long userId);

    List<Expense> findAllByUserId(Long userId);

    @Query("SELECT e From Expense e WHERE e.userId = :userId AND e.createdAt BETWEEN :startDate AND :endDate AND e.category = :category")
    List<Expense> findAllByUserIdAndCreatedAtBetweenAndCategory(Long userId, @Param("startDate") LocalDate from,
                                                                        @Param("endDate") LocalDate to, ExpenseCategory category);

    @Query("SELECT e FROM Expense e WHERE e.userId = :userId ORDER BY e.id DESC")
    List<Expense> findFiveLastByUserId(Long userId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.userId = :userId")
    long countExpensesByUserId(Long userId);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.userId = :userId AND e.createdAt BETWEEN :startDate AND :endDate")
    long countExpensesByUserIdAndCreatedAtBetween(Long userId, @Param("startDate") LocalDate from, @Param("endDate") LocalDate to);

    @Query("SELECT coalesce(sum(e.amount),0) FROM Expense e WHERE e.userId = :userId")
    BigDecimal sumAllExpensesByUserId(Long userId);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId AND e.createdAt >= :startDate AND e.createdAt <= :endDate")
    Optional<BigDecimal> sumExpensesByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT CAST(AVG(e.amount) AS big_decimal) FROM Expense e WHERE e.userId = :userId AND e.createdAt BETWEEN :startDate AND :endDate")
    Optional<BigDecimal> avgExpensesByUserIdAndPeriod(Long userId, @Param("startDate") LocalDate from, @Param("endDate") LocalDate to);

    @Query("""
             SELECT NEW com.finovara.contracts.transaction.report.dto.HighestExpenseDto(
             e.category,
             e.amount
            )
            FROM Expense e WHERE e.userId = :userId AND e.createdAt BETWEEN :from AND :to ORDER BY e.amount DESC
            """)
    List<HighestExpenseDto> findHighestExpensesByUserIdAndPeriod(Long userId, LocalDate from, LocalDate to, Pageable pageable);

    @Query("""
                SELECT new com.finovara.contracts.transaction.report.dto.DailyCashDto(
                    e.createdAt,
                    SUM(e.amount)
                )
                FROM Expense e
                WHERE e.userId = :userId
                GROUP BY e.createdAt
            """)
    List<DailyCashDto> sumExpensesGroupedByDate(Long userId);

    @Query("""
                SELECT new com.finovara.contracts.transaction.report.dto.DailyCashDto(
                    e.createdAt,
                    CAST(AVG(e.amount) AS big_decimal)
                )
                FROM Expense e
                WHERE e.userId = :userId
                GROUP BY e.createdAt
            """)
    List<DailyCashDto> avgExpensesGroupedByDate(Long userId);

}

