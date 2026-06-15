package com.finovara.financeservice.limit.service;

import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.event.activity.limit.LimitActivityEvent;
import com.finovara.contracts.exception.conflict.EntityAlreadyExistsException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.LimitActivityType;
import com.finovara.financeservice.limit.dto.LimitDto;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitManagementService  implements UserDataDeletable {
    private final LimitRepository limitRepository;
    private final LimitCalculateService limitCalculateService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Long createLimit(LimitDto limitDto, Long userId) {
        List<Limit> existingLimit = limitRepository.findByUserIdAndType(userId, limitDto.periodType());

        if (!existingLimit.isEmpty()) {
            throw new EntityAlreadyExistsException("Limit already existing");
        }

        Limit limit = Limit.builder()
                .periodType(limitDto.periodType())
                .amount(limitDto.amount())
                .isActive(true)
                .userId(userId)
                .build();
        kafkaTemplate.send("activity.limit", new LimitActivityEvent(userId,
                LimitActivityType.ADDED_LIMIT, limit.getPeriodType() == null ? null : limit.getPeriodType().name(), limit.getAmount(), null, LocalDateTime.now()));
        Limit savedLimit = limitRepository.save(limit);

        return savedLimit.getId();

    }

    @Transactional
    public Long editLimit(LimitDto limitDto, Long limitId, Long userId) {
        Limit limit = limitRepository.findByIdAndUserId(userId, limitId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Active limit not found"));

        if (!limit.getUserId().equals(userId)) {
            throw new RequestedEntityNotFoundException("Active Limit not found for this user");
        }

        BigDecimal oldLimitAmount = limit.getAmount();

        limit.setPeriodType(limitDto.periodType());
        limit.setAmount(limitDto.amount());

        kafkaTemplate.send("activity.limit", new LimitActivityEvent(userId, LimitActivityType.EDITED_LIMIT, limit.getPeriodType() == null ? null : limit.getPeriodType().name(), limit.getAmount(), oldLimitAmount, LocalDateTime.now()));

        limitRepository.save(limit);
        return limitId;
    }

    public List<LimitStatsDto> getLimitStats(Long userId) {
        List<Limit> limits = limitRepository.findAllByUserId(userId);
        LocalDate today = LocalDate.now();

        return limits.stream()
                .map(limit ->
                        limitCalculateService.calculateLimitStats(userId, limit.getId(), today))
                .toList();
    }

    @Transactional
    public void deleteLimit(Long userId, Long limitId) {
        Limit limit = limitRepository.findByIdAndUserId(userId, limitId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Active limit not found"));
        kafkaTemplate.send("activity.limit", new LimitActivityEvent(userId, LimitActivityType.DELETED_LIMIT, limit.getPeriodType() == null ? null : limit.getPeriodType().name(), limit.getAmount(), null, LocalDateTime.now()));
        limitRepository.delete(limit);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        limitRepository.deleteByUserId(userId);
        log.info("Deleted limit for userId={}", userId);
    }

}
