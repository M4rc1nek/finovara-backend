package com.finovara.financeservice.revenue.service;

import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.mapper.RevenueMapper;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueFilteringService {

    private final RevenueRepository revenueRepository;
    private final RevenueMapper revenueMapper;

    @Value("${finance.pagination.page-size}")
    private int pageSize;

    public List<RevenueDto> revenueFiltering(Long userId, RevenueCategory category, BigDecimal amount, LocalDate date, String description, int page) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.unsorted());

        return revenueRepository.findFilteredRevenues(userId, category, amount, date, description, pageable)
                .stream()
                .map(revenueMapper::mapRevenueToDto)
                .toList();
    }
}
