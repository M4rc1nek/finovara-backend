package com.finovara.finovarabackend.usersetting.finances.recurring.recurring.processor;

/*
@ExtendWith(MockitoExtension.class)
class RecurringRevenueProcessorTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RevenueService revenueService;
    @InjectMocks
    private RecurringProcessor recurringRevenueProcessor;

    private final String EMAIL = "test@example.com";

    @Test
    void shouldGenerateRecurringRevenuesSuccessfully() {
        LocalDate today = LocalDate.now();
        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);

        RecurringSettings settings = RecurringSettings.builder()
                .recurringRevenuesEnable(true)
                .nextExecutionDate(today)
                .recurringAmount(new BigDecimal(100))
                .revenueCategory(RevenueCategory.SALARY)
                .periodType(PeriodType.DAILY)
                .userAssigned(user)
                .build();

        user.setRecurringSettings(settings);

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

        RecurringSettings settings = RecurringSettings.builder()
                .recurringRevenuesEnable(true)
                .nextExecutionDate(today.minusDays(3))
                .recurringAmount(new BigDecimal(100))
                .revenueCategory(RevenueCategory.SALARY)
                .periodType(PeriodType.DAILY)
                .userAssigned(user)
                .build();

        user.setRecurringSettings(settings);

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
*/