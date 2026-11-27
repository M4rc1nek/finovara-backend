package com.finovara.reportservice.savingsplan.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsPlanRequest(
        @NotNull(message = "Kwota celu jest wymagana.")
        @DecimalMin(value = "1.00", message = "Kwota celu musi wynosić co najmniej 1 zł.")
        @DecimalMax(value = "5000000.00", message = "Kwota celu nie może przekraczać 5 000 000 zł.")
        BigDecimal targetAmount,

        @NotNull(message = "Termin osiągnięcia celu jest wymagany.")
        @Future(message = "Termin osiągnięcia celu musi przypadać w przyszłości.")
        LocalDate targetDate,

        @NotNull(message = "Kwota obecnych oszczędności jest wymagana.")
        @DecimalMin(value = "0.00", message = "Obecne oszczędności nie mogą być ujemne.")
        BigDecimal currentSavings,

        @NotNull(message = "Rodzaj przychodu jest wymagany.")
        RevenueType revenueType,

        @NotNull(message = "Strategia oszczędzania jest wymagana.")
        SavingsStrategy strategy
) {
}
