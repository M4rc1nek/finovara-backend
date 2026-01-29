package com.finovara.finovarabackend.limit.service;

import com.finovara.finovarabackend.exception.ActiveLimitNotFoundException;
import com.finovara.finovarabackend.exception.LimitAlreadyExistsException;
import com.finovara.finovarabackend.limit.dto.LimitDTO;
import com.finovara.finovarabackend.limit.dto.LimitStatsDTO;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.model.LimitStatus;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LimitManagementService {
    private final LimitRepository limitRepository;
    private final UserManagerService userManagerService;
    private final LimitService limitService;

    @Transactional
    public Long createLimit(LimitDTO limitDTO, String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        List<Limit> existingLimit = limitRepository.findByUserAssignedIdAndType(user.getId(), limitDTO.limitType());

        if (!existingLimit.isEmpty()) {
            throw new LimitAlreadyExistsException("Limit already existing");
        }

        Limit limit = Limit.builder()
                .limitType(limitDTO.limitType())
                .limitStatus(LimitStatus.NONE)
                .amount(limitDTO.amount())
                .isActive(true)
                .userAssigned(user)
                .build();

        limitRepository.save(limit);

        return limit.getId();

    }

    @Transactional
    public Long editLimit(LimitDTO limitDTO, Long limitId, String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        Limit limit = limitRepository.findByIdAndUserAssignedId(user.getId(), limitId)
                .orElseThrow(() -> new ActiveLimitNotFoundException("Active limit not found"));

        if (limit.getUserAssigned() == null || !limit.getUserAssigned().getId().equals(user.getId())) {
            throw new ActiveLimitNotFoundException("Active Limit not found for this user");
        }

        limit.setLimitType(limitDTO.limitType());
        limit.setAmount(limitDTO.amount());

        limitRepository.save(limit);
        return limitId;
    }

    public List<LimitStatsDTO> getLimitStats(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        List<Limit> limits = limitRepository.findAllByUserAssignedId(user.getId());
        LocalDate today = LocalDate.now();

        return limits.stream()
                .map(limit ->
                        limitService.calculateLimitStats(user.getId(), limit.getId(), today))
                .toList();
    }

    @Transactional
    public void deleteLimit(String email, Long limitId) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        Limit limit = limitRepository.findByIdAndUserAssignedId(user.getId(), limitId)
                .orElseThrow(() -> new ActiveLimitNotFoundException("Active limit not found"));

        limitRepository.delete(limit);
    }

}
