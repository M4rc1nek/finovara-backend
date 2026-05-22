package com.finovara.finovarabackend.limit.service;

import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivityType;
import com.finovara.finovarabackend.accountactivity.limit.service.LimitActivityService;
import com.finovara.finovarabackend.limit.dto.LimitDto;
import com.finovara.finovarabackend.limit.dto.LimitStatsDto;
import com.finovara.finovarabackend.limit.exception.conflict.LimitAlreadyExistsException;
import com.finovara.finovarabackend.limit.exception.notfound.ActiveLimitNotFoundException;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LimitManagementService {
    private final LimitRepository limitRepository;
    private final UserManagerService userManagerService;
    private final LimitCalculateService limitCalculateService;
    private final LimitActivityService limitActivityService;

    @Transactional
    public Long createLimit(LimitDto limitDto, Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        List<Limit> existingLimit = limitRepository.findByUserAssignedIdAndType(user.getId(), limitDto.periodType());

        if (!existingLimit.isEmpty()) {
            throw new LimitAlreadyExistsException("Limit already existing");
        }

        Limit limit = Limit.builder()
                .periodType(limitDto.periodType())
                .amount(limitDto.amount())
                .isActive(true)
                .userAssigned(user)
                .build();
        limitActivityService.createLimitActivity(userId, LimitActivityType.ADDED_LIMIT, limit);

        Limit savedLimit = limitRepository.save(limit);

        return savedLimit.getId();

    }

    @Transactional
    public Long editLimit(LimitDto limitDto, Long limitId, Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        Limit limit = limitRepository.findByIdAndUserAssignedId(user.getId(), limitId)
                .orElseThrow(() -> new ActiveLimitNotFoundException("Active limit not found"));

        if (limit.getUserAssigned() == null || !limit.getUserAssigned().getId().equals(user.getId())) {
            throw new ActiveLimitNotFoundException("Active Limit not found for this user");
        }

        BigDecimal oldLimitAmount = limit.getAmount();

        limit.setPeriodType(limitDto.periodType());
        limit.setAmount(limitDto.amount());

        limitActivityService.updateLimitActivity(userId, LimitActivityType.EDITED_LIMIT, limit, oldLimitAmount);

        limitRepository.save(limit);
        return limitId;
    }

    public List<LimitStatsDto> getLimitStats(Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        List<Limit> limits = limitRepository.findAllByUserAssignedId(user.getId());
        LocalDate today = LocalDate.now();

        return limits.stream()
                .map(limit ->
                        limitCalculateService.calculateLimitStats(user.getId(), limit.getId(), today))
                .toList();
    }

    @Transactional
    public void deleteLimit(Long userId, Long limitId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        Limit limit = limitRepository.findByIdAndUserAssignedId(user.getId(), limitId)
                .orElseThrow(() -> new ActiveLimitNotFoundException("Active limit not found"));
        limitActivityService.createLimitActivity(userId, LimitActivityType.DELETED_LIMIT, limit);
        limitRepository.delete(limit);
    }

}
