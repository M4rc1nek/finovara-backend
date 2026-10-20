package com.finovara.financeservice.util.transaction.revenue;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueManagerServiceTest {

    @Mock
    private RevenueRepository revenueRepository;

    @InjectMocks
    private RevenueManagerService revenueManagerService;

    @Test
    void shouldReturnRevenueWhenExists() {
        Long revenueId = 1L;
        Long userId = 2L;

        Revenue revenue = new Revenue();

        when(revenueRepository.findByIdAndUserId(revenueId, userId)).thenReturn(Optional.of(revenue));

        Revenue result = revenueManagerService.getRevenueByUserIdOrThrow(revenueId, userId);

        assertEquals(revenue, result);
    }

    @Test
    void shouldThrowExceptionWhenRevenueNotFound() {
        Long revenueId = 1L;
        Long userId = 2L;

        when(revenueRepository.findByIdAndUserId(revenueId, userId)).thenReturn(Optional.empty());

        assertThrows(RequestedEntityNotFoundException.class, () -> revenueManagerService.getRevenueByUserIdOrThrow(revenueId, 2L));
        verify(revenueRepository).findByIdAndUserId(revenueId,userId);
    }
}