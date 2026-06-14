package com.finovara.financeservice.revenue.service;

import com.finovara.contracts.event.activity.revenue.RevenueActivityEvent;
import com.finovara.contracts.model.activity.RevenueActivityType;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.mapper.RevenueMapper;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.financeservice.util.revenue.RevenueManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
import com.finovara.financeservice.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {
    @Mock
    private RevenueRepository revenueRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private AutoPaymentsService autoPaymentsService;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private RevenueManagerService revenueManagerService;
    @Mock
    private RevenueMapper revenueMapper;

    @InjectMocks
    private RevenueService revenueService;

    private Long userId;
    @BeforeEach
    void setUp() {
        userId = 1L;
    }

    @Nested
    class AddRevenueTests {
        @Test
        void shouldAddRevenueSuccessfully() {
            RevenueDto dto = new RevenueDto(null, null, new BigDecimal("100"), RevenueCategory.SALARY, null, "test");
            revenueService.addRevenue(dto, userId);

            verify(walletService).addBalanceToWallet(userId, dto.amount());
            ArgumentCaptor<RevenueActivityEvent> eventCaptor = ArgumentCaptor.forClass(RevenueActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.revenue"), eventCaptor.capture());
            assertEquals(RevenueActivityType.ADDED_REVENUE, eventCaptor.getValue().type());
            verify(revenueRepository).save(any(Revenue.class));
            verify(autoPaymentsService).handleRevenuePiggyBankAutomation(userId, dto.amount(), PiggyBankAutomationMode.APPLY);
        }

    }

    @Nested
    class EditRevenueTests {
        @Test
        void shouldEditRevenueSuccessfully() {
            Revenue revenue = new Revenue();
            revenue.setId(10L);
            revenue.setAmount(new BigDecimal("50"));
            revenue.setCategory(RevenueCategory.SALARY);

            RevenueDto dto = new RevenueDto(null, null, new BigDecimal("100"), RevenueCategory.INVESTMENT, null, "edit");

            Wallet wallet = Wallet.create(userId);
            wallet.deposit(new BigDecimal("1000"));
            when(revenueManagerService.getRevenueOrThrow(10L)).thenReturn(revenue);
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

            revenue.setUserId(userId);

            revenueService.editRevenue(dto, 10L, userId);

            verify(autoPaymentsService).handleRevenuePiggyBankAutomation(userId, new BigDecimal("50"), PiggyBankAutomationMode.ROLLBACK);
            verify(autoPaymentsService).handleRevenuePiggyBankAutomation(userId, new BigDecimal("100"), PiggyBankAutomationMode.APPLY);

            ArgumentCaptor<RevenueActivityEvent> eventCaptor = ArgumentCaptor.forClass(RevenueActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.revenue"), eventCaptor.capture());
            assertEquals(RevenueActivityType.EDITED_REVENUE, eventCaptor.getValue().type());

            verify(walletRepository).save(wallet);
            verify(revenueRepository).save(revenue);
        }

        @Test
        void shouldThrowExceptionWhenWalletNotFound() {
            Revenue revenue = new Revenue();
            revenue.setUserId(userId);
            revenue.setId(10L);
            when(revenueManagerService.getRevenueOrThrow(10L)).thenReturn(revenue);
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> revenueService.editRevenue(new RevenueDto(null, null, BigDecimal.TEN, RevenueCategory.SALARY, null, "x"), 10L, userId));

            verify(revenueRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenRevenueBelongsToOtherUser() {
            Revenue revenue = new Revenue();
            revenue.setId(10L);
            revenue.setUserId(2L);

            when(revenueManagerService.getRevenueOrThrow(10L)).thenReturn(revenue);
            assertThrows(RequestedEntityNotFoundException.class, () -> revenueService.editRevenue(new RevenueDto(null, null, BigDecimal.TEN, RevenueCategory.SALARY, null, "x"), 10L, userId));

            verify(revenueRepository, never()).save(any());
        }
    }

    @Nested
    class GetRevenueTests {

        @Test
        void shouldReturnRevenueList() {
            Revenue revenue1 = new Revenue();
            Revenue revenue2 = new Revenue();
            when(revenueRepository.findAllByUserId(userId)).thenReturn(List.of(revenue1, revenue2));
            when(revenueMapper.mapRevenueToDto(any())).thenReturn(new RevenueDto(null, null, BigDecimal.TEN, RevenueCategory.SALARY, null, "x"));

            List<RevenueDto> result = revenueService.getRevenue(userId);

            assertEquals(2, result.size());
            verify(revenueMapper, times(2)).mapRevenueToDto(any());
        }

        @Test
        void shouldReturnEmptyList() {
            when(revenueRepository.findAllByUserId(userId)).thenReturn(List.of());

            List<RevenueDto> result = revenueService.getRevenue(userId);

            assertTrue(result.isEmpty());
            verifyNoInteractions(revenueMapper);
        }
    }

    @Nested
    class DeleteRevenueTests {

        @Test
        void shouldDeleteRevenueSuccessfully() {
            Revenue revenue = new Revenue();
            revenue.setId(1L);
            revenue.setUserId(userId);
            revenue.setAmount(new BigDecimal("100"));
            when(revenueRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(revenue));

            revenueService.deleteRevenue(1L, userId);

            verify(autoPaymentsService).handleRevenuePiggyBankAutomation(userId, new BigDecimal("100"), PiggyBankAutomationMode.ROLLBACK);

            verify(walletService).removeBalanceFromWallet(userId, new BigDecimal("100"));

            ArgumentCaptor<RevenueActivityEvent> eventCaptor = ArgumentCaptor.forClass(RevenueActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.revenue"), eventCaptor.capture());
            assertEquals(RevenueActivityType.DELETED_REVENUE, eventCaptor.getValue().type());

            verify(revenueRepository).delete(revenue);
        }

        @Test
        void shouldThrowWhenRevenueNotFound() {
            when(revenueRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> revenueService.deleteRevenue(1L, userId));

            verify(revenueRepository, never()).delete(any());
        }
    }
}