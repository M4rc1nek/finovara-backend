package com.finovara.corebackend.limit.service;

import com.finovara.activityservice.contracts.event.limit.LimitActivityEvent;
import com.finovara.activityservice.contracts.model.activity.LimitActivityType;
import com.finovara.corebackend.limit.dto.LimitDto;
import com.finovara.corebackend.limit.dto.LimitStatsDto;
import com.finovara.corebackend.limit.exception.conflict.LimitAlreadyExistsException;
import com.finovara.corebackend.limit.exception.notfound.ActiveLimitNotFoundException;
import com.finovara.corebackend.limit.model.Limit;
import com.finovara.corebackend.limit.repository.LimitRepository;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.util.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LimitManagementService {
    private final LimitRepository limitRepository;
    private final UserManagerService userManagerService;
    private final LimitCalculateService limitCalculateService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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
        kafkaTemplate.send("activity.limit", new LimitActivityEvent(userId,
                LimitActivityType.ADDED_LIMIT, limit.getPeriodType() == null ? null : limit.getPeriodType().name(), limit.getAmount(), null, LocalDateTime.now()));
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

        kafkaTemplate.send("activity.limit", new LimitActivityEvent(userId, LimitActivityType.EDITED_LIMIT, limit.getPeriodType() == null ? null : limit.getPeriodType().name(), limit.getAmount(), oldLimitAmount, LocalDateTime.now()));

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
        kafkaTemplate.send("activity.limit", new LimitActivityEvent(userId, LimitActivityType.DELETED_LIMIT, limit.getPeriodType() == null ? null : limit.getPeriodType().name(), limit.getAmount(), null, LocalDateTime.now()));
        limitRepository.delete(limit);
    }

}
