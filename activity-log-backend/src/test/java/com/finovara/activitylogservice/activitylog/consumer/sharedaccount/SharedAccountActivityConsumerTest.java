package com.finovara.activitylogservice.activitylog.sharedaccount.consumer;

import com.finovara.activitylogservice.activitylog.consumer.sharedaccount.SharedAccountActivityConsumer;
import com.finovara.activitylogservice.activitylog.consumer.sharedaccount.SharedAccountActivityDataDeleter;
import com.finovara.activitylogservice.activitylog.sharedaccount.service.SharedAccountActivityLogService;
import com.finovara.activitylogservice.cache.evict.ActivityCache;
import com.finovara.activitylogservice.cache.evict.EvictActivityCacheService;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.contracts.sharedaccount.event.deletion.SharedAccountDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedAccountActivityConsumerTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;

    @Mock
    private SharedAccountActivityDataDeleter sharedAccountActivityDataDeleter;

    @Mock
    private SharedAccountActivityLogService sharedAccountActivityLogService;

    @Mock
    private EvictActivityCacheService evictActivityCacheService;

    @InjectMocks
    private SharedAccountActivityConsumer sharedAccountActivityConsumer;

    @Nested
    class ConsumeSharedAccountActivityLog {

        private SharedAccountActivityLogEvent event;

        @BeforeEach
        void setUp() {
            event = mock(SharedAccountActivityLogEvent.class);
        }

        @Test
        void shouldCreateActivityLogWhenEventIsValid() {
            sharedAccountActivityConsumer.consumeSharedAccountActivityLog(event);

            verify(sharedAccountActivityLogService).createSharedAccountActivityLog(event);
            verifyNoMoreInteractions(sharedAccountActivityLogService);
        }

        @Test
        void shouldNotInteractWithDeleterAndCacheWhenEventIsValid() {
            sharedAccountActivityConsumer.consumeSharedAccountActivityLog(event);

            verifyNoInteractions(sharedAccountActivityDataDeleter, evictActivityCacheService);
        }

        @Test
        void shouldDelegateNullEventWhenEventIsNull() {
            sharedAccountActivityConsumer.consumeSharedAccountActivityLog(null);

            verify(sharedAccountActivityLogService).createSharedAccountActivityLog(null);
        }

        @Test
        void shouldThrowExceptionWhenServiceFails() {
            doThrow(new IllegalStateException()).when(sharedAccountActivityLogService).createSharedAccountActivityLog(event);

            assertThrows(IllegalStateException.class, () -> sharedAccountActivityConsumer.consumeSharedAccountActivityLog(event));
        }
    }

    @Nested
    class DeleteDataFromSharedAccount {

        private SharedAccountDeletedEvent event;

        @BeforeEach
        void setUp() {
            event = mock(SharedAccountDeletedEvent.class);
            when(event.ownerId()).thenReturn(OWNER_ID);
            when(event.memberId()).thenReturn(MEMBER_ID);
        }

        @Test
        void shouldDeleteActivityLogDataWhenEventIsValid() {
            sharedAccountActivityConsumer.deleteDataFromSharedAccount(event);

            verify(sharedAccountActivityDataDeleter).deleteSharedAccountActivityLogData(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldEvictCacheForOwnerAndMemberWhenEventIsValid() {
            sharedAccountActivityConsumer.deleteDataFromSharedAccount(event);

            verify(evictActivityCacheService).evict(ActivityCache.SHARED_ACCOUNT_ACTIVITY_LOG, OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldDeleteDataBeforeEvictingCacheWhenEventIsValid() {
            InOrder inOrder = inOrder(sharedAccountActivityDataDeleter, evictActivityCacheService);

            sharedAccountActivityConsumer.deleteDataFromSharedAccount(event);

            inOrder.verify(sharedAccountActivityDataDeleter).deleteSharedAccountActivityLogData(OWNER_ID, MEMBER_ID);
            inOrder.verify(evictActivityCacheService).evict(ActivityCache.SHARED_ACCOUNT_ACTIVITY_LOG, OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldNotInteractWithActivityLogServiceWhenEventIsValid() {
            sharedAccountActivityConsumer.deleteDataFromSharedAccount(event);

            verifyNoInteractions(sharedAccountActivityLogService);
        }

        @Test
        void shouldThrowExceptionWhenDeleterFails() {
            doThrow(new IllegalStateException()).when(sharedAccountActivityDataDeleter)
                    .deleteSharedAccountActivityLogData(OWNER_ID, MEMBER_ID);

            assertThrows(IllegalStateException.class, () -> sharedAccountActivityConsumer.deleteDataFromSharedAccount(event));
        }

        @Test
        void shouldNotEvictCacheWhenDeleterFails() {
            doThrow(new IllegalStateException()).when(sharedAccountActivityDataDeleter)
                    .deleteSharedAccountActivityLogData(OWNER_ID, MEMBER_ID);

            assertThrows(IllegalStateException.class, () -> sharedAccountActivityConsumer.deleteDataFromSharedAccount(event));

            verify(evictActivityCacheService, never()).evict(ActivityCache.SHARED_ACCOUNT_ACTIVITY_LOG, OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldThrowExceptionWhenCacheEvictionFails() {
            doThrow(new IllegalStateException()).when(evictActivityCacheService)
                    .evict(ActivityCache.SHARED_ACCOUNT_ACTIVITY_LOG, OWNER_ID, MEMBER_ID);

            assertThrows(IllegalStateException.class, () -> sharedAccountActivityConsumer.deleteDataFromSharedAccount(event));
        }
    }
}