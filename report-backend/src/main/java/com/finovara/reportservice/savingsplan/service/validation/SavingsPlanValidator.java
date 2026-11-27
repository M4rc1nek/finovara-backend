package com.finovara.reportservice.savingsplan.service.validation;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.mainaccount.report.savingsplan.dto.SavingsPlanMonthDto;
import com.finovara.reportservice.savingsplan.dto.request.SavingsPlanRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class SavingsPlanValidator {

    public void validate(SavingsPlanRequest request, List<SavingsPlanMonthDto> monthlyData, LocalDate today) {
        if (request == null) {
            throw new InvalidInputException("Dane planu oszczędzania są wymagane.");
        }
        validateRequest(request, today);
        validateRevenueHistory(monthlyData);
    }

    private void validateRequest(SavingsPlanRequest request, LocalDate today) {
        if (request.targetDate() == null || !request.targetDate().isAfter(today)) {
            throw new InvalidInputException("Termin osiągnięcia celu musi przypadać w przyszłości.");
        }
    }

    private void validateRevenueHistory(List<SavingsPlanMonthDto> monthlyData) {
        if (monthlyData == null || monthlyData.isEmpty() || monthlyData.stream().noneMatch(month -> month.revenue().signum() > 0)) {
            throw new InvalidInputException("Nie znaleziono historii przychodów potrzebnej do utworzenia planu.");
        }
    }
}
