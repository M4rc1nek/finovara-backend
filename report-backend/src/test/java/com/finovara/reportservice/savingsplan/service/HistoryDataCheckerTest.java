package com.finovara.reportservice.savingsplan.service;

import com.finovara.contracts.mainaccount.report.savingsplan.dto.SavingsPlanMonthDto;
import com.finovara.reportservice.savingsplan.dto.response.model.DataQuality;
import com.finovara.reportservice.savingsplan.service.history.HistoryDataChecker;
import com.finovara.reportservice.savingsplan.service.money.NumberMath;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryDataCheckerTest {

    private final HistoryDataChecker checker = new HistoryDataChecker(new NumberMath());

    @Test
    void shouldRateFewerThanThreeMonthsAsLow() {
        assertThat(rateHistory("1000", "1000")).isEqualTo(DataQuality.LOW);
    }

    @Test
    void shouldRateThreeOrFourSteadyMonthsAsMedium() {
        assertThat(rateHistory("1000", "1000", "1000")).isEqualTo(DataQuality.MEDIUM);
        assertThat(rateHistory("1000", "1000", "1000", "1000")).isEqualTo(DataQuality.MEDIUM);
    }

    @Test
    void shouldRateFiveSteadyMonthsAsHigh() {
        assertThat(rateHistory("1000", "1000", "1000", "1000", "1000")).isEqualTo(DataQuality.HIGH);
    }

    @Test
    void shouldRateLargeMonthlyChangesAsLow() {
        assertThat(rateHistory("1000", "1000", "1000", "1000", "1000", "10000"))
                .isEqualTo(DataQuality.LOW);
    }

    private DataQuality rateHistory(String... revenues) {
        List<SavingsPlanMonthDto> monthlyData = java.util.stream.IntStream.range(0, revenues.length)
                .mapToObj(index -> new SavingsPlanMonthDto(
                        LocalDate.of(2026, 1, 1).plusMonths(index),
                        new BigDecimal(revenues[index]),
                        Map.of()))
                .toList();
        return checker.checkHistoryQuality(monthlyData,
                monthlyData.stream().map(SavingsPlanMonthDto::revenue).toList());
    }
}
