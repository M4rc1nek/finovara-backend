package com.finovara.corebackend.revenue.service;

import com.finovara.contracts.event.activity.revenue.RevenueActivityEvent;
import com.finovara.contracts.model.activity.RevenueActivityType;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.corebackend.revenue.dto.RevenueDto;
import com.finovara.corebackend.revenue.mapper.RevenueMapper;
import com.finovara.corebackend.revenue.model.Revenue;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.corebackend.revenue.repository.RevenueRepository;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.corebackend.usersetting.piggybank.autopayments.service.AutoPaymentsService;
import com.finovara.corebackend.util.revenue.RevenueManagerService;
import com.finovara.corebackend.util.user.service.UserManagerService;
import com.finovara.corebackend.wallet.model.Wallet;
import com.finovara.corebackend.wallet.repository.WalletRepository;
import com.finovara.corebackend.wallet.service.WalletService;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private UserManagerService userManagerService;
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
    private User user;

    @BeforeEach
    void setUp() {
        userId = 1L;
        user = new User();
        user.setId(userId);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
    }

    @Nested
    class AddRevenueTests {
        @Test
        void shouldAddRevenueSuccessfully() {
            RevenueDto dto = new RevenueDto(null, null, new BigDecimal("100"), RevenueCategory.SALARY, null, "test");

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

            revenueService.addRevenue(dto, userId);

            verify(walletService).addBalanceToWallet(userId, dto.amount());
            ArgumentCaptor<RevenueActivityEvent> eventCaptor = ArgumentCaptor.forClass(RevenueActivityEvent.class);
            verify(kafkaTemplate).send(eq("activity.revenue"), eventCaptor.capture());
            assertEquals(RevenueActivityType.ADDED_REVENUE, eventCaptor.getValue().type());
            verify(revenueRepository).save(any(Revenue.class));
            verify(autoPaymentsService).handleRevenuePiggyBankAutomation(userId, dto.amount(), PiggyBankAutomationMode.APPLY);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            RevenueDto dto = new RevenueDto(null, null, new BigDecimal("100"), RevenueCategory.SALARY, null, "test");

            when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new RequestedEntityNotFoundException("User not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> revenueService.addRevenue(dto, userId));

            verify(revenueRepository, never()).save(any());
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

            Wallet wallet = Wallet.create(user);
            wallet.deposit(new BigDecimal("1000"));

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(revenueManagerService.getRevenueOrThrow(10L)).thenReturn(revenue);
            when(walletRepository.findByUserAssignedId(userId)).thenReturn(Optional.of(wallet));

            revenue.setUserAssigned(user);

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
            revenue.setUserAssigned(user);
            revenue.setId(10L);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(revenueManagerService.getRevenueOrThrow(10L)).thenReturn(revenue);
            when(walletRepository.findByUserAssignedId(userId)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> revenueService.editRevenue(new RevenueDto(null, null, BigDecimal.TEN, RevenueCategory.SALARY, null, "x"), 10L, userId));

            verify(revenueRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenRevenueBelongsToOtherUser() {
            User owner = new User();
            owner.setId(2L);

            Revenue revenue = new Revenue();
            revenue.setId(10L);
            revenue.setUserAssigned(owner);

            when(revenueManagerService.getRevenueOrThrow(10L)).thenReturn(revenue);
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);

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

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(revenueRepository.findAllByUserAssignedId(userId)).thenReturn(List.of(revenue1, revenue2));
            when(revenueMapper.mapRevenueToDto(any())).thenReturn(new RevenueDto(null, null, BigDecimal.TEN, RevenueCategory.SALARY, null, "x"));

            List<RevenueDto> result = revenueService.getRevenue(userId);

            assertEquals(2, result.size());
            verify(revenueMapper, times(2)).mapRevenueToDto(any());
        }

        @Test
        void shouldReturnEmptyList() {

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(revenueRepository.findAllByUserAssignedId(userId)).thenReturn(List.of());

            List<RevenueDto> result = revenueService.getRevenue(userId);

            assertTrue(result.isEmpty());
            verifyNoInteractions(revenueMapper);
        }

        @Test
        void shouldThrowWhenUserNotFound() {

            when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new RequestedEntityNotFoundException("x"));

            assertThrows(RequestedEntityNotFoundException.class, () -> revenueService.getRevenue(userId));

            verifyNoInteractions(revenueRepository);
        }
    }

    @Nested
    class DeleteRevenueTests {

        @Test
        void shouldDeleteRevenueSuccessfully() {
            Revenue revenue = new Revenue();
            revenue.setId(1L);
            revenue.setUserAssigned(user);
            revenue.setAmount(new BigDecimal("100"));

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(revenueRepository.findByIdAndUserAssignedId(1L, userId)).thenReturn(Optional.of(revenue));

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
            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(revenueRepository.findByIdAndUserAssignedId(1L, userId)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> revenueService.deleteRevenue(1L, userId));

            verify(revenueRepository, never()).delete(any());
        }
    }
}