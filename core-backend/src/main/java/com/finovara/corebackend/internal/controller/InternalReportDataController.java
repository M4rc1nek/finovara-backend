package com.finovara.corebackend.internalcontroller;

import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.corebackend.expense.repository.ExpenseRepository;
import com.finovara.corebackend.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/internal/reports")
@RequiredArgsConstructor
public class InternalReportDataController {

    //Move this controller to transaction-service

    private final ExpenseRepository expenseRepository;
    private final RevenueRepository revenueRepository;

    @GetMapping("/expenses/sum")
    public BigDecimal sumExpenses(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return expenseRepository.sumExpensesByUserAndDateRange(userId, from, to).orElse(BigDecimal.ZERO);
    }

    @GetMapping("/expenses/average")
    public BigDecimal avgExpenses(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return expenseRepository.avgExpensesByUserAssignedIdAndPeriod(userId, from, to).orElse(BigDecimal.ZERO);
    }

    @GetMapping("/expenses/highest")
    public List<HighestExpenseDto> highestExpenses(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam int pageSize) {
        return expenseRepository.findHighestExpensesByUserAssignedIdAndPeriod(userId, from, to, PageRequest.of(0, pageSize));
    }

    @GetMapping("/revenues/sum")
    public BigDecimal sumRevenues(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return revenueRepository.sumRevenuesByUserAndDateRange(userId, from, to).orElse(BigDecimal.ZERO);
    }

    @GetMapping("/revenues/average")
    public BigDecimal avgRevenues(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return revenueRepository.avgRevenuesByUserAssignedIdAndPeriod(userId, from, to).orElse(BigDecimal.ZERO);
    }

    @GetMapping("/revenues/highest")
    public List<HighestRevenueDto> highestRevenues(@RequestParam Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to, @RequestParam int pageSize) {
        return revenueRepository.findHighestRevenuesByUserAssignedIdAndPeriod(userId, from, to, PageRequest.of(0, pageSize));
    }

}
