package com.finovara.finovarabackend.usersettings.finances.expense.countlimit.service;

import com.finovara.finovarabackend.exception.StateConflictException;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.model.CountQuantityLimitStrategy;
import com.finovara.finovarabackend.usersettings.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.service.time.SpentInPeriodService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CountQuantityLimitService {

    private final SpentInPeriodService spentInPeriodService;
    private final UserManagerService userManagerService;
    private final ExpenseRepository expenseRepository;

    @Transactional
    public void saveCountQuantityLimit(String email, CountQuantityLimitDto dto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        expenseSettings.setExpenseCountQuantityLimitEnabled(dto.expenseCountLimitEnabled());
        if(!dto.expenseCountLimitEnabled()) return;

        long countedExpenses = countExpensesInPeriod(user, dto.countQuantityLimitStrategy());
        if (dto.numberOfQuantityLimit() < countedExpenses) {
            throw new StateConflictException("You cannot add a limit " + dto.numberOfQuantityLimit() + ", because you have already "
                    + countedExpenses + " expenses in that period");
        }

        expenseSettings.setNumberOfQuantityLimit(dto.numberOfQuantityLimit());
        expenseSettings.setCountQuantityLimitStrategy(dto.countQuantityLimitStrategy());
        log.info("Saved CountQuantityLimit settings. IsEnabled: {}, Limit: {}, Strategy: {}", expenseSettings.isExpenseCountQuantityLimitEnabled(),
                expenseSettings.getNumberOfQuantityLimit(), expenseSettings.getCountQuantityLimitStrategy());
    }

    @Transactional
    public void calculateCountQuantityLimit(String email, CountQuantityLimitDto dto, CountQuantityLimitStrategy strategy) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        if (!expenseSettings.isExpenseCountQuantityLimitEnabled()) return;

        long countedExpenses = countExpensesInPeriod(user, strategy);
        if (countedExpenses + 1 > dto.numberOfQuantityLimit()) {
            log.info("The user's expense exceeds the limit");
            throw new StateConflictException("Quantity Limit Exceeded, you have already added " + countedExpenses + " expenses");
        }
    }

    @Transactional
    public CountQuantityLimitDto getCountQuantityLimit(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        return new CountQuantityLimitDto(expenseSettings.isExpenseCountQuantityLimitEnabled(),
                expenseSettings.getCountQuantityLimitStrategy(), expenseSettings.getNumberOfQuantityLimit());
    }

    private long countExpensesInPeriod(User user, CountQuantityLimitStrategy strategy) {
        LocalDate today = spentInPeriodService.today();
        LocalDate start = switch (strategy) {
            case DAILY -> today;
            case WEEKLY -> today.with(DayOfWeek.MONDAY);
            case MONTHLY -> today.withDayOfMonth(1);
        };

        return expenseRepository.countExpensesByUserAssignedIdAndCreatedAtBetween(user.getId(), start, today);
    }
}

