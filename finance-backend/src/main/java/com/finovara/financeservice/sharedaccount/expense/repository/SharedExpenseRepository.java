package com.finovara.financeservice.sharedaccount.expense.repository;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SharedExpenseRepository extends JpaRepository<SharedExpense, Long> {

    @Query("SELECT se FROM SharedExpense se WHERE se.ownerId = :userId OR se.memberId = :userId")
    List<SharedExpense> findAllByOwnerIdOrMemberId(@Param("userId") Long userId);

    @Query("SELECT se FROM SharedExpense se WHERE se.ownerId = :userId OR se.memberId = :userId")
    SharedExpense findByUserId(Long userId);

    @Query("SELECT se FROM SharedExpense se WHERE se.id = :expenseId AND (se.ownerId = :userId OR se.memberId = :userId)")
    Optional<SharedExpense> findByIdAndOwnerIdOrMemberId(@Param("expenseId") Long expenseId, @Param("userId") Long userId);


    @Query("SELECT SUM(e.amount) FROM SharedExpense e WHERE (e.ownerId = :userId OR e.memberId = :userId) AND e.createdAt >= :startDate AND e.createdAt <= :endDate")
    Optional<BigDecimal> sumExpensesByUsersAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(e.amount) FROM SharedExpense e WHERE (e.ownerId = :userId OR e.memberId = :userId) AND e.createdAt >= :startDate AND e.createdAt <= :endDate AND e.category = :expenseCategory")
    Optional<BigDecimal> sumExpensesByUsersAndDateRangeAndCategory(Long userId, LocalDate startDate, LocalDate endDate, ExpenseCategory expenseCategory);

    @Modifying
    @Query("DELETE FROM SharedExpense se WHERE se.ownerId = :ownerId AND se.memberId = :memberId")
    void deleteAllByOwnerIdAndMemberId(Long ownerId, Long memberId);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0) FROM SharedExpense e
            WHERE e.ownerId = :ownerId AND e.memberId = :memberId AND e.createdByUserId = :userId
            """)
    BigDecimal sumExpenseByCreatedByUserId(Long ownerId, Long memberId, @Param("userId") Long userId);

    @Query("""
            SELECT e FROM SharedExpense e
            WHERE (e.ownerId = :ownerId OR e.memberId = :memberId)
            AND e.createdAt BETWEEN :startDate AND :endDate
            AND e.category = :category
            """)
    List<SharedExpense> findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(Long ownerId, Long memberId, @Param("startDate") LocalDate from, @Param("endDate") LocalDate to, @Param("category") ExpenseCategory category);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0) FROM SharedExpense e
            WHERE e.ownerId = :ownerId OR e.memberId = :memberId
            """)
    BigDecimal sumAllExpensesByOwnerIdOrMemberId(Long ownerId, Long memberId);

    @Query("""
            SELECT SUM(e.amount) FROM SharedExpense e
            WHERE (e.ownerId = :ownerId OR e.memberId = :memberId)
            AND e.createdAt >= :startDate AND e.createdAt <= :endDate
            """)
    Optional<BigDecimal> sumExpensesByOwnerIdOrMemberIdAndDateRange(Long ownerId, Long memberId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
             SELECT NEW com.finovara.contracts.transaction.report.dto.HighestExpenseDto(
             e.category,
             e.amount
            )
            FROM SharedExpense e
            WHERE (e.ownerId = :ownerId OR e.memberId = :memberId)
            AND e.createdAt BETWEEN :from AND :to
            ORDER BY e.amount DESC
            """)
    List<HighestExpenseDto> findHighestExpensesByOwnerIdOrMemberIdAndPeriod(Long ownerId, Long memberId, @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    @Query("""
                SELECT new com.finovara.contracts.transaction.report.dto.DailyCashDto(
                    e.createdAt,
                    SUM(e.amount)
                )
                FROM SharedExpense e
                WHERE e.ownerId = :ownerId OR e.memberId = :memberId
                GROUP BY e.createdAt
            """)
    List<DailyCashDto> sumExpensesGroupedByDateForOwnerIdOrMemberId(Long ownerId, Long memberId);

    @Query("""
                SELECT new com.finovara.contracts.transaction.report.dto.DailyCashDto(
                    e.createdAt,
                    CAST(AVG(e.amount) AS big_decimal)
                )
                FROM SharedExpense e
                WHERE e.ownerId = :ownerId OR e.memberId = :memberId
                GROUP BY e.createdAt
            """)
    List<DailyCashDto> avgExpensesGroupedByDateForOwnerIdOrMemberId(Long ownerId, Long memberId);

    @Query("SELECT e FROM SharedExpense e WHERE (e.ownerId = :userId OR e.memberId = :userId) ORDER BY e.id DESC")
    List<SharedExpense> findTenLastByUserId(Long userId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM SharedExpense e WHERE (e.ownerId = :userId OR e.memberId = :userId)")
    long countExpensesByUserId(Long userId);


}