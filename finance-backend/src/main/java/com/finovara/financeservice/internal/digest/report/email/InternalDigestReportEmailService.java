package com.finovara.financeservice.internal.digest.report.email;

import com.finovara.contracts.notification.email.digest.report.PiggyBankSummaryDto;
import com.finovara.contracts.notification.email.digest.report.WeeklyDigestReportDto;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.internal.digest.report.email.dto.ExpenseSummary;
import com.finovara.financeservice.internal.digest.report.email.dto.RevenueSummary;
import com.finovara.financeservice.internal.digest.report.email.mapper.WeeklyDigestReportMapper;
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
public class InternalDigestReportEmailService {

    private final ExpenseDigestService expenseDigestService;
    private final RevenueDigestService revenueDigestService;
    private final PiggyBankDigestService piggyBankDigestService;
    private final WeeklyDigestReportMapper weeklyDigestReportMapper;
    private final AuthBackendClient authBackendClient;

    public List<WeeklyDigestReportDto> getWeeklyDigestReports() {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        return authBackendClient.getAllUserIds().stream()
                .map(userId -> buildReport(userId, weekStart, weekEnd))
                .toList();
    }

    private WeeklyDigestReportDto buildReport(Long userId, LocalDate from, LocalDate to) {
        ExpenseSummary expenseSummary = expenseDigestService.calculateSummary(userId, from, to);
        RevenueSummary revenueSummary = revenueDigestService.calculateSummary(userId, from, to);
        PiggyBankSummaryDto piggyBankSummary = piggyBankDigestService.calculateSummary(userId, from, to);

        return weeklyDigestReportMapper.toDto(userId, from, to, expenseSummary, revenueSummary, piggyBankSummary);
    }
}