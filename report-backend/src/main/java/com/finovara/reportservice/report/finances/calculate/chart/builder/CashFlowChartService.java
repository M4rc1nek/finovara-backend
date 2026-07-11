package com.finovara.reportservice.report.finances.chart.builder;

import com.finovara.reportservice.report.finances.chart.dto.CashFlowDto;
import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
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
public class CashFlowChartService {

    private final Clock clock;


    public List<CashFlowDto> getCashFlowChart(List<DailyCashDto> expenses, List<DailyCashDto> revenues){
        return getCashFlowChart(expenses, revenues, LocalDate.now(clock));
    }

    public List<CashFlowDto> getCashFlowChart(List<DailyCashDto> expenses, List<DailyCashDto> revenues, LocalDate today) {
        LocalDate startOfMonth = today.withDayOfMonth(1);

        Map<LocalDate, BigDecimal> expenseMap = expenses.stream()
                .collect(Collectors.toMap(DailyCashDto::date, DailyCashDto::amount, BigDecimal::add));

        Map<LocalDate, BigDecimal> revenueMap = revenues.stream()
                .collect(Collectors.toMap(DailyCashDto::date, DailyCashDto::amount, BigDecimal::add));

        List<CashFlowDto> result = new ArrayList<>();

        for (LocalDate date = startOfMonth; !date.isAfter(today); date = date.plusDays(1)) {
            result.add(new CashFlowDto(revenueMap.getOrDefault(date, BigDecimal.ZERO),
                    expenseMap.getOrDefault(date, BigDecimal.ZERO), date));
        }
        return result;
    }
}
