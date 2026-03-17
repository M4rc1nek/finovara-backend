package com.finovara.finovarabackend.util.manager.service.revenue;

import com.finovara.finovarabackend.revenue.exception.notfound.RevenueNotFoundException;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.util.service.revenue.RevenueManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueManagerTest {

    @Mock
    private RevenueRepository revenueRepository;

    @InjectMocks
    private RevenueManagerService revenueManagerService;

    @Test
    void shouldReturnRevenueWhenRevenueIdExists() {
        Revenue revenue = new Revenue();
        revenue.setId(1L);

        when(revenueRepository.findById(1L)).thenReturn(Optional.of(revenue));

        Revenue result = revenueManagerService.getRevenueOrThrow(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void shouldThrowRevenueNotFoundExceptionWhenRevenueIdDoesNotExist() {
        when(revenueRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RevenueNotFoundException.class, () -> revenueManagerService.getRevenueOrThrow(1L));
    }
}