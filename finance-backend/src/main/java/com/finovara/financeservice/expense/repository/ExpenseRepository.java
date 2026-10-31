package com.finovara.financeservice.expense.repository;

import com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestExpenseDto;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.suggestions.dto.ExpenseSuggestionDto;
import com.finovara.financeservice.internal.report.util.dto.ExpenseCategorySumDto;
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

    @Query("SELECT e FROM Expense e WHERE e.userId = :userId ORDER BY e.createdAt DESC, e.id DESC")
    List<Expense> findAllByUserId(Long userId, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.createdAt BETWEEN :startDate AND :endDate AND e.category = :category " +
            "ORDER BY e.createdAt DESC, e.id DESC")
    List<Expense> findAllByUserIdAndCreatedAtBetweenAndCategory(Long userId, @Param("startDate") LocalDate from,
                                                                @Param("endDate") LocalDate to, ExpenseCategory category, Pageable pageable);

    @Query("SELECT e From Expense e WHERE e.userId = :userId AND e.createdAt BETWEEN :startDate AND :endDate")
    List<Expense> findAllByUserIdAndCreatedAtBetween(Long userId, @Param("startDate") LocalDate from,
                                                     @Param("endDate") LocalDate to);
    @Query("""
        SELECT e FROM Expense e
        WHERE e.userId = :userId
          AND (COALESCE(:category, e.category) = e.category)
          AND (COALESCE(:amount, e.amount) = e.amount)
          AND (COALESCE(:createdAt, e.createdAt) = e.createdAt)
          AND (COALESCE(:query, '') = '' OR LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY e.createdAt DESC
        """)
    List<Expense> findFilteredExpenses(Long userId, ExpenseCategory category, BigDecimal amount, LocalDate createdAt, String query, Pageable pageable);
    @Query("""
            SELECT new com.finovara.financeservice.expense.suggestions.dto.ExpenseSuggestionDto(
                e.category,
                e.amount
            )
            FROM Expense e
            WHERE e.userId = :userId
            ORDER BY e.createdAt DESC, e.id DESC
            """)
    List<ExpenseSuggestionDto> findLatestExpensePairsByUserId(Long userId, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.userId = :userId ORDER BY e.createdAt DESC, e.id DESC")
    List<Expense> findLatestByUserId(Long userId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.userId = :userId")
    long countExpensesByUserId(Long userId);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.userId = :userId AND e.createdAt BETWEEN :startDate AND :endDate")
    long countExpensesByUserIdAndCreatedAtBetween(Long userId, @Param("startDate") LocalDate from, @Param("endDate") LocalDate to);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId AND e.createdAt >= :startDate AND e.createdAt <= :endDate")
    Optional<BigDecimal> sumExpensesByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId AND e.createdAt >= :startDate AND e.createdAt <= :endDate AND e.category = :expenseCategory")
    Optional<BigDecimal> sumExpensesByUserAndDateRangeAndCategory(Long userId, LocalDate startDate, LocalDate endDate, ExpenseCategory expenseCategory);

    @Query("""
             SELECT NEW com.finovara.contracts.mainaccount.report.financial.dto.HighestExpenseDto(
             e.category,
             e.amount
            )
            FROM Expense e WHERE e.userId = :userId AND e.createdAt BETWEEN :from AND :to ORDER BY e.amount DESC
            """)
    List<HighestExpenseDto> findHighestExpensesByUserIdAndPeriod(Long userId, LocalDate from, LocalDate to, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.createdAt BETWEEN :from AND :to ORDER BY e.amount DESC")
    List<Expense> findTopExpensesByUserIdAndPeriod(Long userId, LocalDate from, LocalDate to, Pageable pageable);

    @Query("""
            SELECT e.category
            FROM Expense e
            WHERE e.userId = :userId AND e.createdAt BETWEEN :from AND :to
            GROUP BY e.category
            ORDER BY SUM(e.amount) DESC
            """)
    List<ExpenseCategory> findTopExpenseCategoriesByUserIdAndPeriod(Long userId, LocalDate from, LocalDate to, Pageable pageable);

    @Query("""
                SELECT new com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto(
                    e.createdAt,
                    SUM(e.amount)
                )
                FROM Expense e
                WHERE e.userId = :userId
                GROUP BY e.createdAt
            """)
    List<DailyCashDto> sumExpensesGroupedByDate(Long userId);

    @Query("""
            SELECT new com.finovara.financeservice.internal.report.util.dto.ExpenseCategorySumDto(e.category, SUM(e.amount))
            FROM Expense e
            WHERE e.userId = :userId AND e.createdAt BETWEEN :from AND :to
            GROUP BY e.category
            """)
    List<ExpenseCategorySumDto> sumExpensesGroupedByCategory(Long userId, LocalDate from, LocalDate to);

    @Query("""
                SELECT new com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto(
                    e.createdAt,
                    CAST(AVG(e.amount) AS big_decimal)
                )
                FROM Expense e
                WHERE e.userId = :userId
                GROUP BY e.createdAt
            """)
    List<DailyCashDto> avgExpensesGroupedByDate(Long userId);

    List<Expense> findAllByUserId(Long userId);
}
