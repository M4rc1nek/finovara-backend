package com.finovara.reportservice.report.finances.highesttransactions.highestrevenue.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HighestRevenueService {

    private final FinanceBackendReportClient reportClient;
    private final Clock clock;

    @Value("${revenues.highest.page-size}")
    private int pageSize;

    @Cacheable(value = "report:highestRevenue", key = "#userId + ':' + #periodType")
    public List<HighestRevenueDto> getHighestRevenue(Long userId, PeriodType periodType) {
        if (periodType == null) {
            throw new InvalidInputException("Unsupported report period type.");
        }
        LocalDate to = LocalDate.now(clock);
        LocalDate from = periodType.getStartDate(to);
        return reportClient.highestRevenues(userId, from, to, pageSize);
    }
}