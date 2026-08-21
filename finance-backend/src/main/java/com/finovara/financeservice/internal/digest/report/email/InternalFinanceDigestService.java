package com.finovara.financeservice.internal.digest.report.email;

import com.finovara.contracts.notification.email.digest.report.finance.PiggyBankSummaryDto;
import com.finovara.contracts.notification.email.digest.report.finance.WeeklyFinanceDigestReportDto;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.internal.digest.report.email.dto.ExpenseSummary;
import com.finovara.financeservice.internal.digest.report.email.dto.RevenueSummary;
import com.finovara.financeservice.internal.digest.report.email.mapper.WeeklyFinanceDigestReportMapper;
import com.finovara.financeservice.internal.digest.report.email.service.ExpenseDigestService;
import com.finovara.financeservice.internal.digest.report.email.service.PiggyBankDigestService;
import com.finovara.financeservice.internal.digest.report.email.service.RevenueDigestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalFinanceDigestService {

    private final ExpenseDigestService expenseDigestService;
    private final RevenueDigestService revenueDigestService;
    private final PiggyBankDigestService piggyBankDigestService;
    private final WeeklyFinanceDigestReportMapper weeklyFinanceDigestReportMapper;
    private final AuthBackendClient authBackendClient;

    public List<WeeklyFinanceDigestReportDto> getWeeklyFinanceDigestReports() {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        return authBackendClient.getAllUserIds().stream()
                .map(userId -> buildReport(userId, weekStart, weekEnd))
                .toList();
    }

    private WeeklyFinanceDigestReportDto buildReport(Long userId, LocalDate from, LocalDate to) {
        ExpenseSummary expenseSummary = expenseDigestService.calculateSummary(userId, from, to);
        RevenueSummary revenueSummary = revenueDigestService.calculateSummary(userId, from, to);
        PiggyBankSummaryDto piggyBankSummary = piggyBankDigestService.calculateSummary(userId, from, to);

        return weeklyFinanceDigestReportMapper.toDto(userId, from, to, expenseSummary, revenueSummary, piggyBankSummary);
    }
}