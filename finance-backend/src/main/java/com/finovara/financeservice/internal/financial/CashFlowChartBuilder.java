package com.finovara.financeservice.internal.financial;

import com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialCashFlowDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CashFlowChartBuilder {

    public List<FinancialCashFlowDto> build(List<DailyCashDto> expenses, List<DailyCashDto> revenues, LocalDate today) {
        Map<LocalDate, BigDecimal> expenseMap = expenses.stream().collect(Collectors.toMap(DailyCashDto::date, DailyCashDto::amount, BigDecimal::add));
        Map<LocalDate, BigDecimal> revenueMap = revenues.stream().collect(Collectors.toMap(DailyCashDto::date, DailyCashDto::amount, BigDecimal::add));

        List<FinancialCashFlowDto> result = new ArrayList<>();
        for (LocalDate date = today.withDayOfMonth(1); !date.isAfter(today); date = date.plusDays(1)) {
            result.add(new FinancialCashFlowDto(revenueMap.getOrDefault(date, BigDecimal.ZERO), expenseMap.getOrDefault(date, BigDecimal.ZERO), date));
        }
        return result;
    }
}