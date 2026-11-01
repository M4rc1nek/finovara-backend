package com.finovara.financeservice.sharedaccount.revenue.service;

import com.finovara.contracts.sharedaccount.SharedRole;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.revenue.dto.SharedRevenueDto;
import com.finovara.financeservice.sharedaccount.revenue.mapper.SharedRevenueMapper;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenue;
import com.finovara.financeservice.sharedaccount.revenue.repository.SharedRevenueRepository;
import com.finovara.financeservice.util.sharedaccount.SharedUsernameResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SharedRevenueFilteringService {

    private final SharedRevenueRepository sharedRevenueRepository;
    private final SharedAccountParticipantsService sharedAccountParticipantsService;
    private final SharedRevenueMapper sharedRevenueMapper;
    private final SharedUsernameResolver usernameResolver;

    @Value("${finance.pagination.page-size}")
    private int pageSize;

    public List<SharedRevenueDto> filterRevenues(Long userId, RevenueCategory category, BigDecimal amount, LocalDate date,
                                                 String description, SharedRole role, int page) {
        Long createdByUserId = null;
        if (role != null) {
            SharedAccountParticipantsResponse participants = sharedAccountParticipantsService.getParticipants(userId);
            createdByUserId = role == SharedRole.OWNER ? participants.ownerId() : participants.memberId();
        }

        List<SharedRevenue> revenues = sharedRevenueRepository.findFilteredRevenues(userId, createdByUserId, category, amount,
                date, description, PageRequest.of(page, pageSize, Sort.unsorted()));
        Map<Long, String> usernameById = usernameResolver.resolve(
                revenues.stream().map(SharedRevenue::getCreatedByUserId).collect(Collectors.toSet()));

        return revenues.stream()
                .map(revenue -> sharedRevenueMapper.mapToDto(revenue, usernameById.get(revenue.getCreatedByUserId())))
                .toList();
    }
}
