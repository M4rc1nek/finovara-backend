package com.finovara.financeservice.revenue.service;

import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueFilteringServiceTest {

    private static final Long USER_ID = 1L;
    private static final int PAGE_SIZE = 10;
    private static final RevenueCategory CATEGORY = RevenueCategory.SALARY;
    private static final BigDecimal AMOUNT = new BigDecimal("2500.00");
    private static final LocalDate DATE = LocalDate.of(2025, 5, 8);
    private static final String DESCRIPTION = "salary";

    @Mock
    private RevenueRepository revenueRepository;

    @InjectMocks
    private RevenueFilteringService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", PAGE_SIZE);
    }

    @Test
    void shouldFilterRevenuesUsingRepositoryQuery() {
        Revenue revenue = Revenue.builder()
                .id(99L)
                .userId(USER_ID)
                .category(CATEGORY)
                .amount(AMOUNT)
                .createdAt(DATE)
                .description(DESCRIPTION)
                .build();

        when(revenueRepository.findFilteredRevenues(eq(USER_ID), eq(CATEGORY), eq(AMOUNT), eq(DATE), eq(DESCRIPTION), any()))
                .thenReturn(List.of(revenue));

        List<Revenue> result = service.revenueFiltering(USER_ID, CATEGORY, AMOUNT, DATE, DESCRIPTION, 0);

        assertEquals(1, result.size());
        assertSame(revenue, result.get(0));
        verify(revenueRepository).findFilteredRevenues(eq(USER_ID), eq(CATEGORY), eq(AMOUNT), eq(DATE), eq(DESCRIPTION), any());
    }

    @Test
    void shouldUsePageableWithConfiguredPageSizeAndUnsortedSort() {
        when(revenueRepository.findFilteredRevenues(eq(USER_ID), eq(CATEGORY), eq(AMOUNT), eq(DATE), eq(DESCRIPTION), any()))
                .thenReturn(List.of());

        service.revenueFiltering(USER_ID, CATEGORY, AMOUNT, DATE, DESCRIPTION, 2);

        verify(revenueRepository).findFilteredRevenues(eq(USER_ID), eq(CATEGORY), eq(AMOUNT), eq(DATE), eq(DESCRIPTION), any());
    }
}
