package com.finovara.financeservice.expense.repository;

import com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestExpenseDto;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.suggestions.dto.ExpenseSuggestionDto;
import com.finovara.financeservice.internal.report.util.dto.ExpenseCategorySumDto;
import com.finovara.financeservice.limit.dto.LimitSpendingDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByIdAndUserIdAndDeletedAtIsNull(Long expenseId, Long userId);

    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.deletedAt IS NULL ORDER BY e.createdAt DESC, e.id DESC")
    List<Expense> findAllByUserId(Long userId, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.deletedAt IS NULL AND e.createdAt BETWEEN :from AND :to AND e.category = :category ORDER BY e.createdAt DESC, e.id DESC")
    List<Expense> findAllByUserIdAndCreatedAtBetweenAndCategory(Long userId, LocalDate from, LocalDate to, ExpenseCategory category, Pageable pageable);

    @Query("SELECT e From Expense e WHERE e.userId = :userId AND e.deletedAt IS NULL AND e.createdAt BETWEEN :from AND :to")
    List<Expense> findAllByUserIdAndCreatedAtBetween(Long userId, LocalDate from, LocalDate to);
    @Query("""
        SELECT e FROM Expense e
        WHERE e.userId = :userId
          AND e.deletedAt IS NULL
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
              AND e.deletedAt IS NULL
            ORDER BY e.createdAt DESC, e.id DESC
            """)
    List<ExpenseSuggestionDto> findLatestExpensePairsByUserId(Long userId, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.deletedAt IS NULL ORDER BY e.createdAt DESC, e.id DESC")
    List<Expense> findLatestByUserId(Long userId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.userId = :userId AND e.deletedAt IS NULL")
    long countExpensesByUserId(Long userId);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.userId = :userId AND e.deletedAt IS NULL AND e.createdAt BETWEEN :from AND :to")
    long countExpensesByUserIdAndCreatedAtBetween(Long userId, LocalDate from, LocalDate to);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId AND e.deletedAt IS NULL AND e.createdAt >= :from AND e.createdAt <= :to")
    Optional<BigDecimal> sumExpensesByUserAndDateRange(Long userId, LocalDate from, LocalDate to);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId AND e.deletedAt IS NULL AND e.createdAt >= :from AND e.createdAt <= :to AND e.category = :expenseCategory")
    Optional<BigDecimal> sumExpensesByUserAndDateRangeAndCategory(Long userId, LocalDate from, LocalDate to, ExpenseCategory expenseCategory);

    @Query("""
             SELECT NEW com.finovara.contracts.mainaccount.report.financial.dto.HighestExpenseDto(
             e.category,
             e.amount
            )
            FROM Expense e WHERE e.userId = :userId AND e.deletedAt IS NULL AND e.createdAt BETWEEN :from AND :to ORDER BY e.amount DESC
            """)
    List<HighestExpenseDto> findHighestExpensesByUserIdAndPeriod(Long userId, LocalDate from, LocalDate to, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND e.deletedAt IS NULL AND e.createdAt BETWEEN :from AND :to ORDER BY e.amount DESC")
    List<Expense> findTopExpensesByUserIdAndPeriod(Long userId, LocalDate from, LocalDate to, Pageable pageable);

    @Query("""
            SELECT e.category
            FROM Expense e
            WHERE e.userId = :userId AND e.deletedAt IS NULL AND e.createdAt BETWEEN :from AND :to
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
                WHERE e.userId = :userId AND e.deletedAt IS NULL
                GROUP BY e.createdAt
            """)
    List<DailyCashDto> sumExpensesGroupedByDate(Long userId);

    @Query("""
            SELECT new com.finovara.financeservice.internal.report.util.dto.ExpenseCategorySumDto(e.category, SUM(e.amount))
            FROM Expense e
            WHERE e.userId = :userId AND e.deletedAt IS NULL AND e.createdAt BETWEEN :from AND :to
            GROUP BY e.category
            """)
    List<ExpenseCategorySumDto> sumExpensesGroupedByCategory(Long userId, LocalDate from, LocalDate to);

    @Query("""
            SELECT new com.finovara.financeservice.limit.dto.LimitSpendingDto(
                spendingLimit.id, SUM(expense.amount))
            FROM Limit spendingLimit
            LEFT JOIN Expense expense ON expense.userId = spendingLimit.userId
                AND expense.deletedAt IS NULL
                AND expense.createdAt BETWEEN :from AND :to
                AND (spendingLimit.category IS NULL OR expense.category = spendingLimit.category)
            WHERE spendingLimit.id IN :limitIds
            GROUP BY spendingLimit.id
            """)
    List<LimitSpendingDto> sumExpensesByLimitIds(List<Long> limitIds, LocalDate from, LocalDate to);

    @Query("""
                SELECT new com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto(
                    e.createdAt,
                    CAST(AVG(e.amount) AS BigDecimal)
                )
                FROM Expense e
                WHERE e.userId = :userId AND e.deletedAt IS NULL
                GROUP BY e.createdAt
            """)
    List<DailyCashDto> avgExpensesGroupedByDate(Long userId);

    List<Expense> findAllByUserId(Long userId);

    @Query("SELECT DISTINCT e.userId FROM Expense e WHERE e.deletedAt IS NULL")
    List<Long> findUserIdsWithActiveExpenses();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Expense e WHERE e.userId IN :userIds AND e.deletedAt IS NULL ORDER BY e.userId, e.id")
    List<Expense> findAllActiveByUserIdInForUpdate(List<Long> userIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Expense e WHERE e.id = :id AND e.userId = :userId AND e.deletedAt IS NOT NULL")
    Optional<Expense> findTrashedByIdAndUserIdForUpdate(Long id, Long userId);

    @Query("SELECT expense FROM Expense expense WHERE expense.id IN :ids AND expense.deletedAt IS NOT NULL")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Expense> findAllByIdInAndDeletedAtIsNotNull(List<Long> ids);
}
