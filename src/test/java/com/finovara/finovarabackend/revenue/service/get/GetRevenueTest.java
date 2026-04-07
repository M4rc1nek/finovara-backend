package com.finovara.finovarabackend.revenue.service.get;

import com.finovara.finovarabackend.revenue.dto.RevenueDto;
import com.finovara.finovarabackend.revenue.mapper.RevenueMapper;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetRevenueTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private RevenueRepository revenueRepository;
    @Mock
    private RevenueMapper revenueMapper;

    @InjectMocks
    private RevenueService revenueService;

    @Test
    void shouldReturnListOfRevenueDto() {
        String email = "test@test.com";

        User user = new User();
        user.setId(1L);

        Revenue revenue1 = new Revenue();
        Revenue revenue2 = new Revenue();

        RevenueDto dto = new RevenueDto(null, null, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edited revenue test");

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(revenueRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of(revenue1, revenue2));

        when(revenueMapper.mapRevenueToDto(any(Revenue.class))).thenReturn(dto);

        List<RevenueDto> result = revenueService.getRevenue(email);

        // then
        assertEquals(2, result.size());

        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(revenueRepository).findAllByUserAssignedId(user.getId());
        verify(revenueMapper, times(2)).mapRevenueToDto(any(Revenue.class));
    }

    @Test
    void shouldReturnEmptyListWhenNoRevenues() {
        String email = "test@email.com";

        User user = new User();
        user.setId(1L);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(revenueRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of());

        List<RevenueDto> result = revenueService.getRevenue(email);

        assertEquals(0, result.size());
        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(revenueRepository).findAllByUserAssignedId(user.getId());
        verifyNoInteractions(revenueMapper);
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        String email = "test@email.com";

        when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> revenueService.getRevenue(email));
        verify(userManagerService).getUserByEmailOrThrow(email);
        verifyNoInteractions(revenueRepository, revenueMapper);

    }
}
