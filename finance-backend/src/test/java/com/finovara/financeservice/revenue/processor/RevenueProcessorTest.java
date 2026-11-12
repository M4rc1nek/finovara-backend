package com.finovara.financeservice.revenue.processor;

import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.trash.service.TrashMoverService;
import com.finovara.financeservice.wallet.service.WalletService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueProcessorTest {

    private static final Long FIRST_USER_ID = 1L;
    private static final Long SECOND_USER_ID = 2L;

    @Mock
    private RevenueRepository revenueRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private TrashMoverService trashMoverService;

    @InjectMocks
    private RevenueProcessor processor;

    @Nested
    class MoveAllRevenuesToTrash {

        @Test
        void shouldMoveUserRevenuesInBatchesWhenActiveRevenuesExist() {
            Revenue firstUserRevenue = revenue(10L, FIRST_USER_ID);
            Revenue secondUserRevenue = revenue(20L, SECOND_USER_ID);
            when(revenueRepository.findUserIdsWithActiveRevenues()).thenReturn(
                    List.of(SECOND_USER_ID, FIRST_USER_ID));
            when(revenueRepository.findAllActiveByUserIdInForUpdate(List.of(FIRST_USER_ID, SECOND_USER_ID)))
                    .thenReturn(List.of(firstUserRevenue, secondUserRevenue));

            processor.moveAllRevenuesToTrash();

            verify(walletService).lockWalletsForUpdate(List.of(FIRST_USER_ID, SECOND_USER_ID));
            verify(revenueRepository).findAllActiveByUserIdInForUpdate(List.of(FIRST_USER_ID, SECOND_USER_ID));
            verify(trashMoverService).moveRevenuesToTrash(List.of(firstUserRevenue, secondUserRevenue));
        }

        @Test
        void shouldNotQueryOrLockOtherDataWhenThereAreNoActiveRevenues() {
            when(revenueRepository.findUserIdsWithActiveRevenues()).thenReturn(List.of());

            processor.moveAllRevenuesToTrash();

            verifyNoInteractions(walletService, trashMoverService);
            verify(revenueRepository, never()).findAllActiveByUserIdInForUpdate(anyList());
        }
    }

    private Revenue revenue(Long id, Long userId) {
        return Revenue.builder()
                .id(id)
                .userId(userId)
                .amount(BigDecimal.TEN)
                .build();
    }
}
