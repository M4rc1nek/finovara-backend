package com.finovara.finovarabackend.usersettings.finances.expense.countlimit.service;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersettings.finances.expense.countlimit.dto.CountQuantityLimitEmergencyModeDto;
import com.finovara.finovarabackend.usersettings.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CountQuantityLimitEmergencyModeService {

    private final UserManagerService userManagerService;

    @Transactional
    public void saveEmergencyMode(String email, CountQuantityLimitEmergencyModeDto dto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        expenseSettings.setExpenseQuantityLimitEmergencyModeEnabled(dto.expenseQuantityLimitEmergencyModeEnabled());
    }
}
