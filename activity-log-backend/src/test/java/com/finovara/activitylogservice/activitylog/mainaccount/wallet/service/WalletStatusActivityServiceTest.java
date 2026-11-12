package com.finovara.activitylogservice.activitylog.mainaccount.wallet.service;

import com.finovara.activitylogservice.activitylog.mainaccount.wallet.dto.WalletStatusActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.wallet.dto.WalletStatusActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.wallet.mapper.WalletStatusActivityMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.wallet.model.WalletStatusActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.wallet.repository.WalletStatusActivityRepository;
import com.finovara.activitylogservice.activitylog.mainaccount.core.ActivityPageRequest;
import com.finovara.contracts.mainaccount.activity.event.wallet.WalletStatusActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.TransactionType;
import com.finovara.contracts.mainaccount.activity.model.WalletTransactionReason;
import com.finovara.contracts.util.SortType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletStatusActivityServiceTest {

    private static final Long USER_ID = 4L;
    private static final BigDecimal AMOUNT = new BigDecimal("25.50");
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 11, 3, 10, 30);

    @Mock
    private WalletStatusActivityRepository repository;

    @Mock
    private WalletStatusActivityMapper mapper;

    @InjectMocks
    private WalletStatusActivityService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", 100);
    }

    @Test
    void shouldPersistWalletStatusEvent() {
        WalletStatusActivityEvent event = new WalletStatusActivityEvent(USER_ID, TransactionType.WITHDRAWAL,
                WalletTransactionReason.EXPENSE, AMOUNT, OCCURRED_AT);

        service.handleEvent(event);

        ArgumentCaptor<WalletStatusActivity> captor = ArgumentCaptor.forClass(WalletStatusActivity.class);
        verify(repository).save(captor.capture());
        WalletStatusActivity saved = captor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals(TransactionType.WITHDRAWAL, saved.getType());
        assertEquals(WalletTransactionReason.EXPENSE, saved.getReason());
        assertEquals(AMOUNT, saved.getAmount());
        assertEquals(OCCURRED_AT, saved.getCreatedAt());
    }

    @Test
    void shouldFilterByAmountTypeReasonAndDate() {
        WalletStatusActivityFilterDto filter = new WalletStatusActivityFilterDto(
                TransactionType.WITHDRAWAL,
                WalletTransactionReason.EXPENSE,
                AMOUNT,
                OCCURRED_AT.toLocalDate());
        WalletStatusActivity activity = WalletStatusActivity.builder()
                .userId(USER_ID)
                .type(filter.type())
                .reason(filter.reason())
                .amount(filter.amount())
                .createdAt(OCCURRED_AT)
                .build();
        WalletStatusActivityDto dto = new WalletStatusActivityDto(
                filter.type(), filter.reason(), filter.amount(), OCCURRED_AT);
        when(repository.findFilteredActivities(USER_ID, filter, filter.createdAt(), PageRequest.of(0, 100, Sort.unsorted())))
                .thenReturn(List.of(activity));
        when(mapper.mapToDto(activity)).thenReturn(dto);

        List<WalletStatusActivityDto> result = service.filterWalletActivity(USER_ID, filter, 0);

        assertEquals(List.of(dto), result);
    }

    @Test
    void shouldUseSharedActivityPaginationForWalletHistory() {
        WalletStatusActivity activity = WalletStatusActivity.builder().userId(USER_ID).build();
        WalletStatusActivityDto dto = new WalletStatusActivityDto(
                TransactionType.DEPOSIT, WalletTransactionReason.REVENUE, AMOUNT, OCCURRED_AT);
        when(repository.findByUserId(USER_ID, ActivityPageRequest.of(SortType.NEWEST, 0, 100)))
                .thenReturn(List.of(activity));
        when(mapper.mapToDto(activity)).thenReturn(dto);

        List<WalletStatusActivityDto> result = service.getWalletActivity(USER_ID, SortType.NEWEST, 0);

        assertEquals(List.of(dto), result);
    }
}
