package com.finovara.finovarabackend.usersetting.finances.revenue.recurring.processor;

import com.finovara.finovarabackend.revenue.dto.RevenueDto;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.finances.revenue.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.revenue.model.RecurringType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RecurringProcessor {

    private final UserRepository userRepository;
    private final RevenueService revenueService;

    @Transactional
    public void processAll() {

        List<User> users = userRepository.findUsersWithActiveRecurring();
        LocalDate today = LocalDate.now();
        for (User user : users) {
            List<RecurringSettings> settingsList = user.getRecurringSettings();

            if (settingsList == null || settingsList.isEmpty()) {
                continue;
            }

            for (RecurringSettings settings : settingsList) {

                if (!settings.isEnable() || settings.getNextExecutionDate() == null || settings.getPeriodType() == null || settings.getAmount() == null) {
                    continue;
                }

                processSingle(user, settings, today);
            }
        }
    }

    private void processSingle(User user, RecurringSettings settings, LocalDate today) {

        LocalDate nextDate = settings.getNextExecutionDate();

        int safetyCounter = 0;
        int maxIterations = 100;

        while (!nextDate.isAfter(today) && safetyCounter < maxIterations) {

            executeOperation(user, settings, nextDate);

            nextDate = settings.getPeriodType().addPeriod(nextDate);
            safetyCounter++;
        }

        settings.setNextExecutionDate(nextDate);
    }

    private void executeOperation(User user, RecurringSettings settings, LocalDate executionDate) {

        RecurringType type = settings.getType();

        if (type == null) {
            return;
        }

        switch (type) {
            case REVENUE -> createRevenue(user, settings, executionDate);
            case EXPENSE -> createExpense(user, settings, executionDate);
            case SAVINGS -> createSavings(user, settings, executionDate);
        }
    }

    private void createRevenue(User user, RecurringSettings settings, LocalDate date) {

        RevenueDto dto = new RevenueDto(
                null,
                user.getId(),
                settings.getAmount(),
                settings.getRevenueCategory(),
                date,
                "Recurring revenue"
        );

        revenueService.addRevenue(dto, user.getId());
    }

    private void createExpense(User user, RecurringSettings settings, LocalDate date) {
        // TODO: expenseService
        // analogicznie do revenue
    }

    private void createSavings(User user, RecurringSettings settings, LocalDate date) {
        // TODO: savingsService
        // np. wpłata do skarbonki
    }
}