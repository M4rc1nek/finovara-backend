package com.finovara.financeservice.revenue.suggestions.service;

import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.revenue.suggestions.dto.RevenueSuggestionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueSuggestionServiceTest {

    private static final Long USER_ID = 1L;
    private static final int PAGE_SIZE = 3;

    @Mock
    private RevenueRepository revenueRepository;

    @InjectMocks
    private RevenueSuggestionService revenueSuggestionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(revenueSuggestionService, "pageSize", PAGE_SIZE);
    }

    @Nested
    class GetRevenueSuggestion {

        @Test
        void shouldReturnSuggestionWhenAllLatestRevenuesAreIdentical() {
            RevenueSuggestionDto suggestion = new RevenueSuggestionDto(RevenueCategory.SALARY, BigDecimal.valueOf(5000));
            List<RevenueSuggestionDto> latest = List.of(suggestion, suggestion, suggestion);
            when(revenueRepository.findLatestRevenuePairsByUserId(eq(USER_ID), any(PageRequest.class))).thenReturn(latest);

            RevenueSuggestionDto result = revenueSuggestionService.getRevenueSuggestion(USER_ID);

            assertEquals(suggestion, result);
        }

        @Test
        void shouldReturnNullWhenLatestRevenuesCountIsLessThanPageSize() {
            List<RevenueSuggestionDto> latest = List.of(new RevenueSuggestionDto(RevenueCategory.SALARY, BigDecimal.valueOf(5000)));
            when(revenueRepository.findLatestRevenuePairsByUserId(eq(USER_ID), any(PageRequest.class))).thenReturn(latest);

            RevenueSuggestionDto result = revenueSuggestionService.getRevenueSuggestion(USER_ID);

            assertNull(result);
        }

        @Test
        void shouldReturnNullWhenLatestRevenuesAreNotIdentical() {
            List<RevenueSuggestionDto> latest = List.of(
                    new RevenueSuggestionDto(RevenueCategory.SALARY, BigDecimal.valueOf(5000)),
                    new RevenueSuggestionDto(RevenueCategory.GIFT, BigDecimal.valueOf(5000)),
                    new RevenueSuggestionDto(RevenueCategory.SALARY, BigDecimal.valueOf(5000))
            );
            when(revenueRepository.findLatestRevenuePairsByUserId(eq(USER_ID), any(PageRequest.class))).thenReturn(latest);

            RevenueSuggestionDto result = revenueSuggestionService.getRevenueSuggestion(USER_ID);

            assertNull(result);
        }

        @Test
        void shouldReturnNullWhenRepositoryReturnsEmptyList() {
            when(revenueRepository.findLatestRevenuePairsByUserId(eq(USER_ID), any(PageRequest.class))).thenReturn(Collections.emptyList());

            RevenueSuggestionDto result = revenueSuggestionService.getRevenueSuggestion(USER_ID);

            assertNull(result);
        }

        @Test
        void shouldReturnSuggestionWhenPageSizeIsOneAndSingleRevenueExists() {
            ReflectionTestUtils.setField(revenueSuggestionService, "pageSize", 1);
            RevenueSuggestionDto suggestion = new RevenueSuggestionDto(RevenueCategory.GIFT, BigDecimal.TEN);
            when(revenueRepository.findLatestRevenuePairsByUserId(eq(USER_ID), any(PageRequest.class))).thenReturn(List.of(suggestion));

            RevenueSuggestionDto result = revenueSuggestionService.getRevenueSuggestion(USER_ID);

            assertEquals(suggestion, result);
        }

        @Test
        void shouldCallRepositoryWithCorrectUserIdAndPageableExactlyOnce() {
            when(revenueRepository.findLatestRevenuePairsByUserId(eq(USER_ID), any(PageRequest.class))).thenReturn(Collections.emptyList());

            revenueSuggestionService.getRevenueSuggestion(USER_ID);

            verify(revenueRepository, times(1)).findLatestRevenuePairsByUserId(eq(USER_ID), eq(PageRequest.of(0, PAGE_SIZE)));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryThrowsException() {
            when(revenueRepository.findLatestRevenuePairsByUserId(eq(USER_ID), any(PageRequest.class))).thenThrow(new RuntimeException("Database error"));

            assertThrows(RuntimeException.class, () -> revenueSuggestionService.getRevenueSuggestion(USER_ID));
        }

        @Test
        void shouldReturnNullWhenUserIdIsNullAndRepositoryReturnsEmptyList() {
            when(revenueRepository.findLatestRevenuePairsByUserId(isNull(), any(PageRequest.class))).thenReturn(Collections.emptyList());

            RevenueSuggestionDto result = revenueSuggestionService.getRevenueSuggestion(null);

            assertNull(result);
        }
    }
}