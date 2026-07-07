package com.finovara.financeservice.sharedaccount.repository.expense;

import com.finovara.financeservice.sharedaccount.model.expense.SharedExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SharedExpenseRepository extends JpaRepository<SharedExpense, Long> {

    @Query("SELECT se FROM SharedExpense se WHERE se.ownerId = :userId OR se.memberId = :userId")
    List<SharedExpense> findAllByOwnerIdOrMemberId(Long userId);

    @Query("SELECT se FROM SharedExpense se WHERE se.id = :expenseId AND (se.ownerId = :userId OR se.memberId = :userId)")
    Optional<SharedExpense> findByIdAndOwnerIdOrMemberId(Long expenseId, Long userId);

    @Modifying
    @Query("DELETE FROM SharedExpense se WHERE se.ownerId = :ownerId AND se.memberId = :memberId")
    void deleteAllByOwnerIdAndMemberId(Long ownerId, Long memberId);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0) FROM SharedExpense e
            WHERE e.ownerId = :ownerId AND e.memberId = :memberId AND e.createdByUserId = :userId
            """)
    BigDecimal sumExpenseByCreatedByUserId(Long ownerId, Long memberId, Long userId);

}