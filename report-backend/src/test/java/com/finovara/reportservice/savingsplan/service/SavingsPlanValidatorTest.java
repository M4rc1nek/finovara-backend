package com.finovara.reportservice.savingsplan.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.mainaccount.report.savingsplan.dto.SavingsPlanMonthDto;
import com.finovara.reportservice.savingsplan.dto.request.RevenueType;
import com.finovara.reportservice.savingsplan.dto.request.SavingsPlanRequest;
import com.finovara.reportservice.savingsplan.dto.request.SavingsStrategy;
import com.finovara.reportservice.savingsplan.service.validation.SavingsPlanValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SavingsPlanValidatorTest {

    private final SavingsPlanValidator validator = new SavingsPlanValidator();
    private final List<SavingsPlanMonthDto> monthlyData = List.of(
            new SavingsPlanMonthDto(LocalDate.of(2026, 1, 1), new BigDecimal("1000"), Map.of())
    );

    @Test
    void shouldAllowTargetDateOneDayInTheFuture() {
        LocalDate today = LocalDate.of(2026, 1, 31);

        assertThatCode(() -> validator.validate(request(today.plusDays(1)), monthlyData, today))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectTargetDateToday() {
        LocalDate today = LocalDate.of(2026, 1, 31);

        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> validator.validate(request(today), monthlyData, today));

        org.assertj.core.api.Assertions.assertThat(exception.getMessage())
                .isEqualTo("Termin osiągnięcia celu musi przypadać w przyszłości.");
    }

    @Test
    void shouldExplainMissingRevenueHistoryInPolish() {
        InvalidInputException exception = assertThrows(
                InvalidInputException.class,
                () -> validator.validate(request(LocalDate.of(2026, 2, 1)), List.of(), LocalDate.of(2026, 1, 31)));

        org.assertj.core.api.Assertions.assertThat(exception.getMessage())
                .isEqualTo("Nie znaleziono historii przychodów potrzebnej do utworzenia planu.");
    }

    @Test
    void shouldAllowTargetDateInTheNextMonth() {
        LocalDate today = LocalDate.of(2026, 3, 30);

        assertThatCode(() -> validator.validate(request(LocalDate.of(2026, 4, 30)), monthlyData, today))
                .doesNotThrowAnyException();
    }

    private SavingsPlanRequest request(LocalDate targetDate) {
        return new SavingsPlanRequest(
                new BigDecimal("1000"),
                targetDate,
                BigDecimal.ZERO,
                RevenueType.STABLE,
                SavingsStrategy.BALANCED
        );
    }
}
