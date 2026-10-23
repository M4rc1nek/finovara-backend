package com.finovara.financeservice.settings.finances.recurring.service.occurrence;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringOccurrenceDto;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringOccurrenceService {

    private static final int MAX_OCCURRENCES_PER_RULE = 1000;

    @Value("${scheduler.recurring-settings.max-occurrence-range-days}")
    private long maxRangeDay;

    private final RecurringSettingsRepository recurringSettingsRepository;

    @Transactional(readOnly = true)
    public List<RecurringOccurrenceDto> getUpcomingOccurrences(Long userId, LocalDate from, LocalDate to) {
        validateDateRange(from, to);

        List<RecurringSettings> activeRecurringRules = recurringSettingsRepository.findAllEnabledByUserId(userId);

        return activeRecurringRules.stream()
                .filter(rule -> rule.getNextExecutionDate() != null && rule.getPeriodType() != null)
                .flatMap(rule -> generateOccurrencesForRule(rule, from, to).stream())
                .sorted(Comparator.comparing(RecurringOccurrenceDto::date))
                .toList();
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidInputException("'from' date must not be after 'to' date");
        }

        if (ChronoUnit.DAYS.between(from, to) > maxRangeDay) {
            throw new InvalidInputException("Date range cannot exceed " + maxRangeDay + " days");
        }
    }

    private List<RecurringOccurrenceDto> generateOccurrencesForRule(RecurringSettings recurringSettings, LocalDate from, LocalDate to) {
        List<RecurringOccurrenceDto> occurrencesInRange = new ArrayList<>();
        LocalDate nextOccurrenceDate = recurringSettings.getNextExecutionDate();
        LocalDate endDate = recurringSettings.getEndDate();
        int iterationGuard = 0;

        while (!nextOccurrenceDate.isAfter(to) && (endDate == null || !nextOccurrenceDate.isAfter(endDate)) && iterationGuard++ < MAX_OCCURRENCES_PER_RULE) {
            if (!nextOccurrenceDate.isBefore(from)) {
                occurrencesInRange.add(mapToOccurrenceDto(recurringSettings, nextOccurrenceDate));
            }
            nextOccurrenceDate = recurringSettings.getPeriodType().addPeriod(nextOccurrenceDate);
        }

        return occurrencesInRange;
    }

    private RecurringOccurrenceDto mapToOccurrenceDto(RecurringSettings rule, LocalDate occurrenceDate) {
        return new RecurringOccurrenceDto(
                occurrenceDate,
                rule.getType(),
                rule.getAmount(),
                rule.getExpenseCategory(),
                rule.getRevenueCategory(),
                rule.getId(),
                rule.getPiggyBankId()
        );
    }
}