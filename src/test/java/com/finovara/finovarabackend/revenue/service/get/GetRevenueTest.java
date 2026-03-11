package com.finovara.finovarabackend.revenue.service.get;

import com.finovara.finovarabackend.revenue.dto.RevenueDTO;
import com.finovara.finovarabackend.revenue.mapper.RevenueMapper;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GetRevenueTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private RevenueRepository revenueRepository;
    @Mock
    private RevenueMapper revenueMapper;

    @InjectMocks
    private RevenueService revenueService;

    @Test
    void shouldReturnListOfRevenueDTO() {
        String email = "test@test.com";

        User user = new User();
        user.setId(1L);

        Revenue revenue1 = new Revenue();
        Revenue revenue2 = new Revenue();

        RevenueDTO dto = new RevenueDTO(null, null, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edited revenue test");

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(revenueRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of(revenue1, revenue2));

        when(revenueMapper.mapRevenueToDTO(any(Revenue.class))).thenReturn(dto);

        List<RevenueDTO> result = revenueService.getRevenue(email);

        // then
        assertEquals(2, result.size());

        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(revenueRepository).findAllByUserAssignedId(user.getId());
        verify(revenueMapper, times(2)).mapRevenueToDTO(any(Revenue.class));
    }

    @Test
    void shouldReturnEmptyListWhenNoRevenues() {
        String email = "test@email.com";

        User user = new User();
        user.setId(1L);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(revenueRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of());

        List<RevenueDTO> result = revenueService.getRevenue(email);

        assertEquals(0, result.size());
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        String email = "test@email.com";

        when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> revenueService.getRevenue(email));
        verify(revenueRepository, never()).save(any());

    }
}
