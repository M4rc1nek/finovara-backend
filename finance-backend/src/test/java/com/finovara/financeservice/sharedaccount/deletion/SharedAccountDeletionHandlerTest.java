package com.finovara.financeservice.sharedaccount.deletion;

import com.finovara.contracts.event.finance.sharedaccount.SharedAccountDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedAccountDeletionHandlerTest {

    @Mock
    private SharedAccountDeletionFinanceDataService financeDataService;

    @Mock
    private WalletCacheEvictionService cacheEvictionService;

    private SharedAccountDeletionHandler handler;

    private Long ownerId;
    private Long memberId;
    private SharedAccountDeletedEvent event;

    @BeforeEach
    void setUp() {
        handler = new SharedAccountDeletionHandler(financeDataService, cacheEvictionService);
        ownerId = 1L;
        memberId = 2L;
        event = mock(SharedAccountDeletedEvent.class);
    }

    @Nested
    class WhenDataWasDeleted {

        @BeforeEach
        void stubDeletion() {
            when(event.ownerId()).thenReturn(ownerId);
            when(event.memberId()).thenReturn(memberId);
            when(financeDataService.deleteData(event)).thenReturn(true);
        }

        @Test
        void shouldEvictCacheWhenDataWasDeleted() {
            handler.handle(event);

            verify(cacheEvictionService).evictAfterSharedAccountDeletion(ownerId, memberId);
        }
    }

    @Nested
    class WhenDataWasNotDeleted {

        @BeforeEach
        void stubDuplicateDeletion() {
            when(financeDataService.deleteData(event)).thenReturn(false);
        }

        @Test
        void shouldNotEvictCacheWhenDataWasNotDeleted() {
            handler.handle(event);

            verifyNoInteractions(cacheEvictionService);
        }
    }

    @Nested
    class WhenFinanceDataServiceThrowsException {

        @BeforeEach
        void stubException() {
            when(financeDataService.deleteData(event)).thenThrow(new RuntimeException("database unavailable"));
        }

        @Test
        void shouldThrowExceptionWhenFinanceDataServiceFails() {
            assertThrows(RuntimeException.class, () -> handler.handle(event));
        }

        @Test
        void shouldNotEvictCacheWhenFinanceDataServiceThrowsException() {
            assertThrows(RuntimeException.class, () -> handler.handle(event));

            verifyNoInteractions(cacheEvictionService);
        }
    }
}