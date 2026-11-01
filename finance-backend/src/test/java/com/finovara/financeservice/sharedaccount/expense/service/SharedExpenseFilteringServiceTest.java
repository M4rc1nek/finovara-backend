package com.finovara.financeservice.sharedaccount.expense.service;

import com.finovara.contracts.sharedaccount.SharedRole;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseDto;
import com.finovara.financeservice.sharedaccount.expense.mapper.SharedExpenseMapper;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.util.sharedaccount.SharedUsernameResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedExpenseFilteringServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final int PAGE_SIZE = 10;

    @Mock
    private SharedExpenseRepository sharedExpenseRepository;
    @Mock
    private SharedAccountParticipantsService participantsService;
    @Mock
    private SharedExpenseMapper sharedExpenseMapper;
    @Mock
    private SharedUsernameResolver usernameResolver;
    @Mock
    private SharedAccountParticipantsResponse participants;
    @Mock
    private SharedExpenseDto expenseDto;

    @InjectMocks
    private SharedExpenseFilteringService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", PAGE_SIZE);
    }

    @Test
    void shouldFilterAndMapExpensesByFieldsAndOwnerRole() {
        ExpenseCategory category = ExpenseCategory.values()[0];
        BigDecimal amount = BigDecimal.TEN;
        LocalDate date = LocalDate.now();
        SharedExpense expense = SharedExpense.builder().createdByUserId(OWNER_ID).build();
        when(participantsService.getParticipants(USER_ID)).thenReturn(participants);
        when(participants.ownerId()).thenReturn(OWNER_ID);
        when(sharedExpenseRepository.findFilteredExpenses(USER_ID, OWNER_ID, category, amount, date, "food",
                PageRequest.of(1, PAGE_SIZE))).thenReturn(List.of(expense));
        when(usernameResolver.resolve(Set.of(OWNER_ID))).thenReturn(Map.of(OWNER_ID, "owner"));
        when(sharedExpenseMapper.mapToDto(expense, "owner")).thenReturn(expenseDto);

        List<SharedExpenseDto> result = service.filterExpenses(USER_ID, category, amount, date, "food", SharedRole.OWNER, 1);

        assertEquals(List.of(expenseDto), result);
    }

    @Test
    void shouldNotResolveParticipantsWhenRoleIsNotSpecified() {
        when(sharedExpenseRepository.findFilteredExpenses(USER_ID, null, null, null, null, null,
                PageRequest.of(0, PAGE_SIZE))).thenReturn(List.of());

        List<SharedExpenseDto> result = service.filterExpenses(USER_ID, null, null, null, null, null, 0);

        assertEquals(List.of(), result);
        verify(sharedExpenseRepository).findFilteredExpenses(USER_ID, null, null, null, null, null,
                PageRequest.of(0, PAGE_SIZE));
        verifyNoInteractions(participantsService);
    }
}
