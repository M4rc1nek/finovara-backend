package com.finovara.finovarabackend.report.finances.chart.builder;

import com.finovara.finovarabackend.report.finances.chart.dto.CashFlowDto;
import com.finovara.finovarabackend.report.finances.chart.dto.DailyCashDto;
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
public class CashFlowChartBuilder {
    private final Clock clock;

    public List<CashFlowDto> getCashFlowChartBuilder(List<DailyCashDto> expenses, List<DailyCashDto> revenues) {
        LocalDate today = LocalDate.now(clock);
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
