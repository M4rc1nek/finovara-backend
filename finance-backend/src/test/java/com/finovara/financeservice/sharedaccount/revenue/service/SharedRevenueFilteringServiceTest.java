package com.finovara.financeservice.sharedaccount.revenue.service;

import com.finovara.contracts.sharedaccount.SharedRole;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.revenue.dto.SharedRevenueDto;
import com.finovara.financeservice.sharedaccount.revenue.mapper.SharedRevenueMapper;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenue;
import com.finovara.financeservice.sharedaccount.revenue.repository.SharedRevenueRepository;
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
class SharedRevenueFilteringServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final int PAGE_SIZE = 10;

    @Mock
    private SharedRevenueRepository sharedRevenueRepository;
    @Mock
    private SharedAccountParticipantsService participantsService;
    @Mock
    private SharedRevenueMapper sharedRevenueMapper;
    @Mock
    private SharedUsernameResolver usernameResolver;
    @Mock
    private SharedAccountParticipantsResponse participants;
    @Mock
    private SharedRevenueDto revenueDto;

    @InjectMocks
    private SharedRevenueFilteringService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", PAGE_SIZE);
    }

    @Test
    void shouldFilterAndMapRevenuesByFieldsAndMemberRole() {
        RevenueCategory category = RevenueCategory.values()[0];
        BigDecimal amount = BigDecimal.TEN;
        LocalDate date = LocalDate.now();
        SharedRevenue revenue = SharedRevenue.builder().createdByUserId(MEMBER_ID).build();
        when(participantsService.getParticipants(USER_ID)).thenReturn(participants);
        when(participants.memberId()).thenReturn(MEMBER_ID);
        when(sharedRevenueRepository.findFilteredRevenues(USER_ID, MEMBER_ID, category, amount, date, "salary",
                PageRequest.of(2, PAGE_SIZE))).thenReturn(List.of(revenue));
        when(usernameResolver.resolve(Set.of(MEMBER_ID))).thenReturn(Map.of(MEMBER_ID, "member"));
        when(sharedRevenueMapper.mapToDto(revenue, "member")).thenReturn(revenueDto);

        List<SharedRevenueDto> result = service.filterRevenues(USER_ID, category, amount, date, "salary", SharedRole.MEMBER, 2);

        assertEquals(List.of(revenueDto), result);
    }

    @Test
    void shouldNotResolveParticipantsWhenRoleIsNotSpecified() {
        when(sharedRevenueRepository.findFilteredRevenues(USER_ID, null, null, null, null, null,
                PageRequest.of(0, PAGE_SIZE))).thenReturn(List.of());

        List<SharedRevenueDto> result = service.filterRevenues(USER_ID, null, null, null, null, null, 0);

        assertEquals(List.of(), result);
        verify(sharedRevenueRepository).findFilteredRevenues(USER_ID, null, null, null, null, null,
                PageRequest.of(0, PAGE_SIZE));
        verifyNoInteractions(participantsService);
    }
}
