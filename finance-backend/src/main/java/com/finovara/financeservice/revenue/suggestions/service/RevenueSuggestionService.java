package com.finovara.financeservice.revenue.suggestions.service;

import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.revenue.suggestions.dto.RevenueSuggestionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueSuggestionService {

    private final RevenueRepository revenueRepository;

    @Value("${revenue.suggestion.history-size}")
    private int pageSize;

    @Cacheable(value = "revenue:suggestion", key = "#userId", unless = "#result == null")
    public RevenueSuggestionDto getRevenueSuggestion(Long userId) {
        List<RevenueSuggestionDto> latest = revenueRepository.findLatestRevenuePairsByUserId(userId, PageRequest.of(0, pageSize));

        if (latest.size() < pageSize) {
            return null;
        }

        boolean allIdentical = latest.stream().distinct().count() == 1;
        return allIdentical ? latest.getFirst() : null;
    }
}
