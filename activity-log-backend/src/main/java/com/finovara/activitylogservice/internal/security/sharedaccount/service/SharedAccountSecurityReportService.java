package com.finovara.activitylogservice.internal.security.sharedaccount.service;

import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.activitylogservice.internal.security.sharedaccount.repository.SharedAccountFinanceActivityRepository;
import com.finovara.activitylogservice.internal.security.util.clientinfo.ClientInfoResolver;
import com.finovara.activitylogservice.internal.security.util.clientinfo.dto.ClientInfoDto;
import com.finovara.contracts.activity.event.sharedaccount.SharedFinanceActivityType;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.report.dto.security.SharedAccountSecurityOverviewDto;
import com.finovara.contracts.report.dto.security.SharedAccountSecurityReportDto;
import com.finovara.contracts.sharedaccount.SharedAccountMemberInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SharedAccountSecurityReportService {

    private final ClientInfoResolver clientInfoResolver;
    private final AuthBackendClient authBackendClient;
    private final SharedAccountFinanceActivityRepository financeActivityRepository;

    public SharedAccountSecurityOverviewDto getSecurityOverview(Long callerId, PeriodType periodType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = periodType.getStartDate(now.toLocalDate()).atStartOfDay();

        List<SharedAccountSecurityReportDto> members = authBackendClient.getSharedAccountMembers(callerId).stream()
                .sorted(Comparator.comparing((SharedAccountMemberInfoDto member) -> !member.userId().equals(callerId)))
                .map(member -> buildMemberReport(member, from, now))
                .toList();

        return new SharedAccountSecurityOverviewDto(members);
    }

    private SharedAccountSecurityReportDto buildMemberReport(SharedAccountMemberInfoDto member, LocalDateTime from, LocalDateTime to) {
        Long userId = member.userId();
        ClientInfoDto clientInfo = clientInfoResolver.getClientContext(userId, from, to);

        int expensesCount = countActivities(userId, SharedFinanceActivityType.EXPENSE_CREATED, from, to);
        LocalDateTime lastExpenseDate = lastActivityDate(userId, SharedFinanceActivityType.EXPENSE_CREATED);

        int revenuesCount = countActivities(userId, SharedFinanceActivityType.REVENUE_CREATED, from, to);
        LocalDateTime lastRevenueDate = lastActivityDate(userId, SharedFinanceActivityType.REVENUE_CREATED);

        int piggyBankDepositsCount = countActivities(userId, SharedFinanceActivityType.PIGGY_BANK_DEPOSIT, from, to);
        LocalDateTime lastPiggyBankDepositDate = lastActivityDate(userId, SharedFinanceActivityType.PIGGY_BANK_DEPOSIT);

        LocalDateTime lastActiveAt = financeActivityRepository.findLastActivityDateByUserId(userId);

        return new SharedAccountSecurityReportDto(
                userId,
                member.username(),
                member.role(),
                clientInfo.locationShares(),
                clientInfo.browserShares(),
                expensesCount,
                lastExpenseDate,
                revenuesCount,
                lastRevenueDate,
                piggyBankDepositsCount,
                lastPiggyBankDepositDate,
                lastActiveAt
        );
    }

    private int countActivities(Long userId, SharedFinanceActivityType type, LocalDateTime from, LocalDateTime to) {
        return financeActivityRepository.countByUserIdAndActivityType(userId, type, from, to);
    }

    private LocalDateTime lastActivityDate(Long userId, SharedFinanceActivityType type) {
        return financeActivityRepository.findLastActivityDateByUserIdAndActivityType(userId, type);
    }
}