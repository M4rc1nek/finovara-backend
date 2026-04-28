package com.finovara.finovarabackend.usersetting.finances.revenue.recurring.service;

import static org.mockito.Mockito.verify;
/*
@ExtendWith(MockitoExtension.class)
class RecurringRevenueServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private RecurringSettingsService recurringRevenueService;

    private User user;
    private RecurringSettings recurringSettings;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        user = new User();
        recurringSettings = new RecurringSettings();
        user.setRecurringSettings(recurringSettings);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
    }

    @Nested
    class SaveRecurringRevenueTest {

        @Test
        void shouldEnableRecurringRevenue() {
            LocalDate startDate = LocalDate.of(2026, 3, 1);

            recurringSettingsDto dto = new recurringSettingsDto(true, BigDecimal.valueOf(500), RevenueCategory.SALARY, PeriodType.MONTHLY, startDate, null);

            recurringRevenueService.saveRecurringRevenue(USER_ID, dto);

            assertTrue(recurringSettings.isRecurringRevenuesEnable());
            assertEquals(BigDecimal.valueOf(500), recurringSettings.getRecurringAmount());
            assertEquals(RevenueCategory.SALARY, recurringSettings.getRevenueCategory());
            assertEquals(PeriodType.MONTHLY, recurringSettings.getPeriodType());
            assertEquals(startDate, recurringSettings.getRecurringStartDate());
            assertEquals(startDate, recurringSettings.getNextExecutionDate());

            verify(settingsActivityService).createSettingActivity(USER_ID, SettingActivityStatus.ENABLED, SettingType.REVENUE_RECURRING);
        }

        @Test
        void shouldDisableRecurringRevenue() {
            LocalDate startDate = LocalDate.of(2026, 3, 1);

            recurringSettingsDto dto = new recurringSettingsDto(false, BigDecimal.valueOf(500), RevenueCategory.SALARY, PeriodType.MONTHLY, startDate, null);

            recurringRevenueService.saveRecurringRevenue(USER_ID, dto);

            assertFalse(recurringSettings.isRecurringRevenuesEnable());
            assertEquals(BigDecimal.valueOf(500), recurringSettings.getRecurringAmount());
            assertEquals(RevenueCategory.SALARY, recurringSettings.getRevenueCategory());
            assertEquals(PeriodType.MONTHLY, recurringSettings.getPeriodType());
            assertNull(recurringSettings.getNextExecutionDate());

            verify(settingsActivityService).createSettingActivity(USER_ID, SettingActivityStatus.DISABLED, SettingType.REVENUE_RECURRING);
        }
    }

    @Nested
    class GetRecurringRevenueTest {
        @Test
        void shouldReturnRecurringRevenueWhenEnabled() {
            LocalDate startDate = LocalDate.of(2026, 3, 1);

            recurringSettings.setRecurringRevenuesEnable(true);
            recurringSettings.setRecurringAmount(BigDecimal.valueOf(500));
            recurringSettings.setRevenueCategory(RevenueCategory.SALARY);
            recurringSettings.setPeriodType(PeriodType.MONTHLY);
            recurringSettings.setRecurringStartDate(startDate);
            recurringSettings.setNextExecutionDate(startDate.plusDays(1));

            recurringSettingsDto dto = recurringRevenueService.getRecurringRevenue(USER_ID);

            assertTrue(dto.enable());
            assertEquals(BigDecimal.valueOf(500), dto.amount());
            assertEquals(RevenueCategory.SALARY, dto.category());
            assertEquals(PeriodType.MONTHLY, dto.periodType());
            assertEquals(startDate, dto.startDate());
            assertEquals(startDate.plusDays(1), dto.nextExecutionDate());
        }

        @Test
        void shouldReturnRecurringRevenueWhenDisabled() {
            LocalDate startDate = LocalDate.of(2026, 3, 1);

            recurringSettings.setRecurringRevenuesEnable(false);
            recurringSettings.setRecurringAmount(BigDecimal.valueOf(200));
            recurringSettings.setRevenueCategory(RevenueCategory.BONUS);
            recurringSettings.setPeriodType(PeriodType.WEEKLY);
            recurringSettings.setRecurringStartDate(startDate);
            recurringSettings.setNextExecutionDate(null);

            recurringSettingsDto dto = recurringRevenueService.getRecurringRevenue(USER_ID);

            assertFalse(dto.enable());
            assertEquals(BigDecimal.valueOf(200), dto.amount());
            assertEquals(RevenueCategory.BONUS, dto.category());
            assertEquals(PeriodType.WEEKLY, dto.periodType());
            assertEquals(startDate, dto.startDate());
            assertNull(dto.nextExecutionDate());
        }
    }
}

*/