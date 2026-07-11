package com.finovara.reportservice.sharedaccount.report.finances.chart.builder;

import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.reportservice.sharedaccount.report.finances.chart.dto.SharedCashFlowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SharedCashFlowChartService {

    private final Clock clock;

    public List<SharedCashFlowDto> getSharedCashFlowChart(List<DailyCashDto> expenses, List<DailyCashDto> revenues) {
        return getSharedCashFlowChart(expenses, revenues, LocalDate.now(clock));
    }

    public List<SharedCashFlowDto> getSharedCashFlowChart(List<DailyCashDto> expenses, List<DailyCashDto> revenues, LocalDate today) {
        LocalDate startOfMonth = today.withDayOfMonth(1);

        Map<LocalDate, BigDecimal> expenseMap = expenses.stream()
                .collect(Collectors.toMap(DailyCashDto::date, DailyCashDto::amount, BigDecimal::add));

        Map<LocalDate, BigDecimal> revenueMap = revenues.stream()
                .collect(Collectors.toMap(DailyCashDto::date, DailyCashDto::amount, BigDecimal::add));

        List<SharedCashFlowDto> result = new ArrayList<>();

        for (LocalDate date = startOfMonth; !date.isAfter(today); date = date.plusDays(1)) {
            result.add(new SharedCashFlowDto(revenueMap.getOrDefault(date, BigDecimal.ZERO),
                    expenseMap.getOrDefault(date, BigDecimal.ZERO), date));
        }
        return result;
    }
}
