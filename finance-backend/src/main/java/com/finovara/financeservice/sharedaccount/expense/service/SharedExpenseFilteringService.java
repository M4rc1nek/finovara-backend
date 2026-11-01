package com.finovara.financeservice.sharedaccount.expense.service;

import com.finovara.contracts.sharedaccount.SharedRole;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseDto;
import com.finovara.financeservice.sharedaccount.expense.mapper.SharedExpenseMapper;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.util.sharedaccount.SharedUsernameResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SharedExpenseFilteringService {

    private final SharedExpenseRepository sharedExpenseRepository;
    private final SharedAccountParticipantsService sharedAccountParticipantsService;
    private final SharedExpenseMapper sharedExpenseMapper;
    private final SharedUsernameResolver usernameResolver;

    @Value("${finance.pagination.page-size}")
    private int pageSize;

    public List<SharedExpenseDto> filterExpenses(Long userId, ExpenseCategory category, BigDecimal amount, LocalDate date,
                                                 String description, SharedRole role, int page) {
        Long createdByUserId = null;
        if (role != null) {
            SharedAccountParticipantsResponse participants = sharedAccountParticipantsService.getParticipants(userId);
            createdByUserId = role == SharedRole.OWNER ? participants.ownerId() : participants.memberId();
        }

        List<SharedExpense> expenses = sharedExpenseRepository.findFilteredExpenses(userId, createdByUserId, category, amount,
                date, description, PageRequest.of(page, pageSize, Sort.unsorted()));
        Map<Long, String> usernameById = usernameResolver.resolve(
                expenses.stream().map(SharedExpense::getCreatedByUserId).collect(Collectors.toSet()));

        return expenses.stream()
                .map(expense -> sharedExpenseMapper.mapToDto(expense, usernameById.get(expense.getCreatedByUserId())))
                .toList();
    }
}
