package com.finovara.finovarabackend.usersetting.finances.revenue.recurring.processor;

import com.finovara.finovarabackend.revenue.dto.RevenueDTO;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.finances.revenue.model.RevenueSettings;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RecurringRevenueProcessor {

    private final UserRepository userRepository;
    private final RevenueService revenueService;

    @Transactional
    public void generateRecurringRevenues() {
        List<User> users = userRepository.findAll();
        LocalDate today = LocalDate.now();

        for (User user : users) {
            RevenueSettings settings = user.getRevenueSettings();
            if(settings == null) continue;
            while(settings.isRecurringRevenuesEnable() && settings.getNextExecutionDate() != null && !settings.getNextExecutionDate().isAfter(today)){
                createRecurringRevenue(user, settings);
                switch (settings.getRecurringStrategy()) {
                    case DAILY -> settings.setNextExecutionDate(settings.getNextExecutionDate().plusDays(1));
                    case WEEKLY -> settings.setNextExecutionDate(settings.getNextExecutionDate().plusWeeks(1));
                    case MONTHLY -> settings.setNextExecutionDate(settings.getNextExecutionDate().plusMonths(1));
                }
            }
        }
    }

    private void createRecurringRevenue(User user, RevenueSettings settings) {
        RevenueDTO dto = new RevenueDTO(
                null,
                settings.getUserAssigned().getId(),
                settings.getRecurringAmount(),
                settings.getRevenueCategory(),
                settings.getNextExecutionDate(),
                "Cykliczny przychód"
        );

        revenueService.addRevenue(dto, user.getEmail());
    }

}
