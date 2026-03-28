package com.finovara.finovarabackend.report.finances.highestrevenue.service;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.report.finances.highestrevenue.dto.HighestRevenueDto;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.util.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HighestRevenueService {

    private final RevenueRepository revenueRepository;

    @Value("${revenues.highest.page-size}")
    private int pageSize;

    public List<HighestRevenueDto> getHighestRevenue(Long userId, PeriodType periodType) {
        if (periodType == null) {
            throw new InvalidInputException("Unsupported report period type.");
        }
        LocalDate today = LocalDate.now();
        LocalDate from;
        switch (periodType) {
            case DAILY -> from = today;
            case WEEKLY -> from = today.with(DayOfWeek.MONDAY);
            case MONTHLY -> from = today.withDayOfMonth(1);
            default -> throw new IllegalStateException("Unexpected value: " + periodType);
        }

        return revenueRepository.findHighestRevenuesByUserAssignedIdAndPeriod(userId, from, today, PageRequest.of(0, pageSize));

    }
}
