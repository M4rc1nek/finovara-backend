package com.finovara.finovarabackend.usersetting.finances.revenue.recurring.processor;

import com.finovara.finovarabackend.revenue.dto.RevenueDto;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.finances.revenue.model.RevenueSettings;
import com.finovara.finovarabackend.util.model.PeriodType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringRevenueProcessorTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RevenueService revenueService;
    @InjectMocks
    private RecurringRevenueProcessor recurringRevenueProcessor;

    private final String EMAIL = "test@example.com";

    @Test
    void shouldGenerateRecurringRevenuesSuccessfully() {
        LocalDate today = LocalDate.now();
        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);

        RevenueSettings settings = RevenueSettings.builder()
                .recurringRevenuesEnable(true)
                .nextExecutionDate(today)
                .recurringAmount(new BigDecimal(100))
                .revenueCategory(RevenueCategory.SALARY)
                .periodType(PeriodType.DAILY)
                .userAssigned(user)
                .build();

        user.setRevenueSettings(settings);

        when(userRepository.findAll()).thenReturn(List.of(user));

        recurringRevenueProcessor.generateRecurringRevenues();

        verify(revenueService).addRevenue(
                new RevenueDto(
                        null,
                        user.getId(),
                        new BigDecimal(100),
                        RevenueCategory.SALARY,
                        today,
                        "Cykliczny przychód"
                ),
                user.getId()
        );
    }

    @Test
    void shouldGenerateMultipleRevenuesUntilToday() {
        LocalDate today = LocalDate.now();
        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);

        RevenueSettings settings = RevenueSettings.builder()
                .recurringRevenuesEnable(true)
                .nextExecutionDate(today.minusDays(3))
                .recurringAmount(new BigDecimal(100))
                .revenueCategory(RevenueCategory.SALARY)
                .periodType(PeriodType.DAILY)
                .userAssigned(user)
                .build();

        user.setRevenueSettings(settings);

        when(userRepository.findAll()).thenReturn(List.of(user));

        recurringRevenueProcessor.generateRecurringRevenues();

        verify(revenueService, times(4)) // today-3, today-2, today-1, today
                .addRevenue(any(RevenueDto.class), eq(user.getId()));
    }

    @Test
    void shouldSkipUserWithoutSettings() {
        User user = new User();
        user.setEmail(EMAIL);

        when(userRepository.findAll()).thenReturn(List.of(user));

        recurringRevenueProcessor.generateRecurringRevenues();

        verify(revenueService, never()).addRevenue(any(), anyLong());
    }
}
