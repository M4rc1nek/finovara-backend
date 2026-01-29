package com.finovara.finovarabackend.util.service.time;

import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SpentInPeriodService {
    private final TimeConfig timeConfig;
    private final ExpenseRepository expenseRepository;

    public LocalDate today() {
        return LocalDate.now(timeConfig.clock());
    }

    public BigDecimal getSpentToday(Long userId) {
        LocalDate today = today();
        return getSpentInPeriod(userId, today, today);
    }

    public BigDecimal getSpentWeekly(Long userId) {
        LocalDate today = today();
        LocalDate firstDayOfWeek = today.with(DayOfWeek.MONDAY);
        return getSpentInPeriod(userId, firstDayOfWeek, today);
    }

    public BigDecimal getSpentMonthly(Long userId) {
        LocalDate today = today();
        LocalDate startMonth = today.withDayOfMonth(1);
        return getSpentInPeriod(userId, startMonth, today);
    }

    private BigDecimal getSpentInPeriod(Long userId, LocalDate from, LocalDate to) {
        BigDecimal spent = expenseRepository.sumExpensesByUserAndDateRange(userId, from, to);
        return spent != null ? spent : BigDecimal.ZERO;
    }
}
