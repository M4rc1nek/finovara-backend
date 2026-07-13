package com.finovara.financeservice.sharedaccount.revenue.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.sharedaccount.revenue.dto.SharedRevenueDto;
import com.finovara.financeservice.sharedaccount.revenue.dto.SharedRevenueResponse;
import com.finovara.financeservice.sharedaccount.wallet.dto.SharedWalletDto;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenue;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.revenue.mapper.SharedRevenueMapper;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.revenue.SharedRevenueManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedRevenueServiceTest {

    @Mock
    private SharedRevenueMapper sharedRevenueMapper;

    @Mock
    private SharedRevenueRepository sharedRevenueRepository;

    @Mock
    private SharedWalletService sharedWalletService;

    @Mock
    private SharedRevenueManagerService sharedRevenueManagerService;

    @Mock
    private AuthBackendClient authBackendClient;

    @InjectMocks
    private SharedRevenueService sharedRevenueService;

    private Long userId;
    private SharedRevenueDto sharedRevenueDto;

    @BeforeEach
    void setUp() {
        userId = 1L;
        sharedRevenueDto = mock(SharedRevenueDto.class);
    }

    @Nested
    class AddSharedRevenue {

        private SharedWalletDto walletDto;
        private Long ownerId;
        private Long memberId;
        private BigDecimal amount;
        private RevenueCategory category;
        private String description;
        private String username;

        @BeforeEach
        void setUp() {
            ownerId = 1L;
            memberId = 2L;
            amount = new BigDecimal("150.00");
            category = RevenueCategory.BONUS;
            description = "Groceries";
            username = "testuser";

            walletDto = mock(SharedWalletDto.class);
            when(walletDto.ownerId()).thenReturn(ownerId);
            when(walletDto.memberId()).thenReturn(memberId);

            when(sharedWalletService.getWallet(userId)).thenReturn(walletDto);
            when(authBackendClient.getUsername(userId)).thenReturn(username);

            when(sharedRevenueDto.amount()).thenReturn(amount);
            when(sharedRevenueDto.category()).thenReturn(category);
            when(sharedRevenueDto.description()).thenReturn(description);
        }

        @Test
        void shouldAddSharedRevenueAndReturnResponse() {
            SharedRevenueResponse response = sharedRevenueService.addSharedRevenue(sharedRevenueDto, userId);

            verify(sharedWalletService).addBalanceToWallet(userId, amount);
            verify(sharedRevenueRepository).save(any(SharedRevenue.class));

            assertEquals(userId, response.userId());
            assertEquals(username, response.username());
            assertNull(response.revenueId() );
        }
    }

    @Nested
    class EditRevenue {

        private Long revenueId;
        private Long ownerId;
        private Long memberId;
        private SharedRevenue existingRevenue;
        private BigDecimal oldAmount;
        private BigDecimal newAmount;
        private RevenueCategory category;
        private String description;

        @BeforeEach
        void setUp() {
            revenueId = 5L;
            ownerId = 1L;
            memberId = 2L;
            oldAmount = new BigDecimal("100.00");
            newAmount = new BigDecimal("200.00");
            category = RevenueCategory.SALARY;
            description = "Updated description";

            existingRevenue = mock(SharedRevenue.class);
        }

        @Test
        void shouldEditRevenueWhenUserIsOwner() {
            when(sharedRevenueManagerService.getSharedRevenueOrThrow(revenueId)).thenReturn(existingRevenue);
            when(existingRevenue.getOwnerId()).thenReturn(ownerId);
            when(existingRevenue.getAmount()).thenReturn(oldAmount);

            when(sharedRevenueDto.amount()).thenReturn(newAmount);
            when(sharedRevenueDto.category()).thenReturn(category);
            when(sharedRevenueDto.description()).thenReturn(description);

            Long result = sharedRevenueService.editRevenue(sharedRevenueDto, revenueId, ownerId);

            assertEquals(revenueId, result);
            verify(sharedWalletService).addBalanceToWallet(ownerId, newAmount);
            verify(sharedWalletService).removeBalanceFromWallet(ownerId, oldAmount);
            verify(existingRevenue).setAmount(newAmount);
            verify(existingRevenue).setCategory(category);
            verify(existingRevenue).setDescription(description);
            verify(sharedRevenueRepository).save(existingRevenue);
        }

        @Test
        void shouldEditRevenueWhenUserIsMember() {
            when(sharedRevenueManagerService.getSharedRevenueOrThrow(revenueId)).thenReturn(existingRevenue);
            when(existingRevenue.getOwnerId()).thenReturn(ownerId);
            when(existingRevenue.getMemberId()).thenReturn(memberId);
            when(existingRevenue.getAmount()).thenReturn(oldAmount);

            when(sharedRevenueDto.amount()).thenReturn(newAmount);
            when(sharedRevenueDto.category()).thenReturn(category);
            when(sharedRevenueDto.description()).thenReturn(description);

            Long result = sharedRevenueService.editRevenue(sharedRevenueDto, revenueId, memberId);

            assertEquals(revenueId, result);
            verify(sharedWalletService).addBalanceToWallet(memberId, newAmount);
            verify(sharedWalletService).removeBalanceFromWallet(memberId, oldAmount);
            verify(existingRevenue).setAmount(newAmount);
            verify(existingRevenue).setCategory(category);
            verify(existingRevenue).setDescription(description);
            verify(sharedRevenueRepository).save(existingRevenue);
        }

        @Test
        void shouldThrowWhenUserIsNeitherOwnerNorMember() {
            Long strangerId = 99L;

            when(sharedRevenueManagerService.getSharedRevenueOrThrow(revenueId)).thenReturn(existingRevenue);
            when(existingRevenue.getOwnerId()).thenReturn(ownerId);
            when(existingRevenue.getMemberId()).thenReturn(memberId);

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> sharedRevenueService.editRevenue(sharedRevenueDto, revenueId, strangerId));

            verify(sharedWalletService, never()).addBalanceToWallet(any(), any());
            verify(sharedWalletService, never()).removeBalanceFromWallet(any(), any());
            verify(sharedRevenueRepository, never()).save(any());
        }
    }

    @Nested
    class GetRevenue {

        @Test
        void shouldReturnMappedRevenuesGroupedByUsername() {
            SharedRevenue revenueOne = mock(SharedRevenue.class);
            SharedRevenue revenueTwo = mock(SharedRevenue.class);
            SharedRevenue revenueThree = mock(SharedRevenue.class);

            when(revenueOne.getCreatedByUserId()).thenReturn(10L);
            when(revenueTwo.getCreatedByUserId()).thenReturn(10L);
            when(revenueThree.getCreatedByUserId()).thenReturn(20L);

            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(userId))
                    .thenReturn(List.of(revenueOne, revenueTwo, revenueThree));

            when(authBackendClient.getUsername(10L)).thenReturn("alice");
            when(authBackendClient.getUsername(20L)).thenReturn("bob");

            SharedRevenueDto dtoOne = mock(SharedRevenueDto.class);
            SharedRevenueDto dtoTwo = mock(SharedRevenueDto.class);
            SharedRevenueDto dtoThree = mock(SharedRevenueDto.class);

            when(sharedRevenueMapper.mapToDto(revenueOne, "alice")).thenReturn(dtoOne);
            when(sharedRevenueMapper.mapToDto(revenueTwo, "alice")).thenReturn(dtoTwo);
            when(sharedRevenueMapper.mapToDto(revenueThree, "bob")).thenReturn(dtoThree);

            List<SharedRevenueDto> result = sharedRevenueService.getRevenue(userId);

            assertEquals(List.of(dtoOne, dtoTwo, dtoThree), result);
        }

        @Test
        void shouldReturnEmptyListWhenNoRevenues() {
            when(sharedRevenueRepository.findAllByOwnerIdOrMemberId(userId)).thenReturn(List.of());

            List<SharedRevenueDto> result = sharedRevenueService.getRevenue(userId);

            assertTrue(result.isEmpty());
            verify(authBackendClient, never()).getUsername(any());
            verify(sharedRevenueMapper, never()).mapToDto(any(), any());
        }
    }

    @Nested
    class DeleteRevenue {

        private Long revenueId;
        private SharedRevenue revenue;
        private BigDecimal amount;

        @BeforeEach
        void setUp() {
            revenueId = 5L;
            amount = new BigDecimal("75.00");
            revenue = mock(SharedRevenue.class);
        }

        @Test
        void shouldDeleteRevenueAndRemoveBalance() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(revenueId, userId))
                    .thenReturn(Optional.of(revenue));
            when(revenue.getAmount()).thenReturn(amount);

            sharedRevenueService.deleteRevenue(revenueId, userId);

            verify(sharedWalletService).removeBalanceFromWallet(userId, amount);
            verify(sharedRevenueRepository).delete(revenue);
        }

        @Test
        void shouldThrowWhenRevenueNotFound() {
            when(sharedRevenueRepository.findByIdAndOwnerIdOrMemberId(revenueId, userId))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> sharedRevenueService.deleteRevenue(revenueId, userId));

            verify(sharedWalletService, never()).removeBalanceFromWallet(any(), any());
            verify(sharedRevenueRepository, never()).delete(any());
        }
    }
}