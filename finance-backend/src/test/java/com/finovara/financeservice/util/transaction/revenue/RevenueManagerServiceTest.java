package com.finovara.financeservice.util.revenue;

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

        Revenue revenue = new Revenue();

        when(revenueRepository.findById(revenueId)).thenReturn(Optional.of(revenue));

        Revenue result = revenueManagerService.getRevenueOrThrow(revenueId);

        assertEquals(revenue, result);
        verify(revenueRepository).findById(revenueId);
    }

    @Test
    void shouldThrowExceptionWhenRevenueNotFound() {
        Long revenueId = 1L;

        when(revenueRepository.findById(revenueId)).thenReturn(Optional.empty());

        assertThrows(RequestedEntityNotFoundException.class, () -> revenueManagerService.getRevenueOrThrow(revenueId));

        verify(revenueRepository).findById(revenueId);
    }
}