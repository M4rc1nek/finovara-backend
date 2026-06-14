package com.finovara.authbackend.usersetting.finances.expense.countlimit.service;

import com.finovara.authbackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitEmergencyModeDto;
import com.finovara.authbackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.authbackend.usersetting.finances.expense.repository.ExpenseSettingsRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CountQuantityLimitEmergencyModeService {

    private final ExpenseSettingsRepository expenseSettingsRepository;

    @Transactional
    public void saveEmergencyMode(Long userId, CountQuantityLimitEmergencyModeDto dto) {
        ExpenseSettings expenseSettings = expenseSettingsRepository.findByUserIdOrThrow(userId);

        expenseSettings.setQuantityLimitEmergencyModeEnabled(dto.expenseQuantityLimitEmergencyModeEnabled());
    }
}
