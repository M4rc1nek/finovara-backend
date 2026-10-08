package com.finovara.activitylogservice.internal.security.sharedaccount.service;

import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.activitylogservice.activitylog.sharedaccount.repository.SharedAccountActivityLogRepository;
import com.finovara.activitylogservice.internal.security.util.clientinfo.ClientInfoResolver;
import com.finovara.activitylogservice.internal.security.util.clientinfo.dto.ClientInfoDto;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.sharedaccount.report.security.dto.SharedAccountSecurityOverviewDto;
import com.finovara.contracts.sharedaccount.report.security.dto.SharedAccountSecurityReportDto;
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
    private final SharedAccountActivityLogRepository sharedAccountActivityLogRepository;

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

        int expensesCount = countActivities(userId, SharedAccountActivityLogType.EXPENSE_CREATED, from, to);
        LocalDateTime lastExpenseDate = lastActivityDate(userId, SharedAccountActivityLogType.EXPENSE_CREATED);

        int revenuesCount = countActivities(userId, SharedAccountActivityLogType.REVENUE_CREATED, from, to);
        LocalDateTime lastRevenueDate = lastActivityDate(userId, SharedAccountActivityLogType.REVENUE_CREATED);

        int piggyBankDepositsCount = countActivities(userId, SharedAccountActivityLogType.PIGGY_BANK_DEPOSIT, from, to);
        LocalDateTime lastPiggyBankDepositDate = lastActivityDate(userId, SharedAccountActivityLogType.PIGGY_BANK_DEPOSIT);

        LocalDateTime lastActiveAt = sharedAccountActivityLogRepository.findLastActivityDateByUserId(userId);

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

    private int countActivities(Long userId, SharedAccountActivityLogType type, LocalDateTime from, LocalDateTime to) {
        return sharedAccountActivityLogRepository.countByUserIdAndActivityType(userId, type, from, to);
    }

    private LocalDateTime lastActivityDate(Long userId, SharedAccountActivityLogType type) {
        return sharedAccountActivityLogRepository.findLastActivityDateByUserIdAndActivityType(userId, type);
    }
}