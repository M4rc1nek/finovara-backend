package com.finovara.finovarabackend.usersetting.finances.expense.countlimit.service;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitEmergencyModeDto;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CountQuantityLimitEmergencyModeService {

    private final UserManagerService userManagerService;

    @Transactional
    public void saveEmergencyMode(Long userId, CountQuantityLimitEmergencyModeDto dto) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        expenseSettings.setQuantityLimitEmergencyModeEnabled(dto.expenseQuantityLimitEmergencyModeEnabled());
    }
}