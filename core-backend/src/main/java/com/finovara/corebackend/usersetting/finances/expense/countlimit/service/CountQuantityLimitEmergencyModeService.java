package com.finovara.corebackend.usersetting.finances.expense.countlimit.service;

import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitEmergencyModeDto;
import com.finovara.corebackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.corebackend.util.user.service.UserManagerService;
import org.springframework.transaction.annotation.Transactional;
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