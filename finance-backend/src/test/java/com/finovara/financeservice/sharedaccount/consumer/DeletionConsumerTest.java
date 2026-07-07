package com.finovara.financeservice.sharedaccount.consumer;

import com.finovara.contracts.event.finance.sharedaccount.SharedAccountDeletedEvent;
import com.finovara.financeservice.sharedaccount.service.deletion.SharedAccountDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeletionConsumerTest {

    private static final Long ACCOUNT_ID = 100L;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long REMAINING_USER_ID = 2L;

    @Mock
    private SharedAccountDeletionService sharedAccountDeletionService;

    private DeletionConsumer deletionConsumer;

    private SharedAccountDeletedEvent event;

    @BeforeEach
    void setUp() {
        deletionConsumer = new DeletionConsumer(sharedAccountDeletionService);
        event = new SharedAccountDeletedEvent(ACCOUNT_ID, OWNER_ID, MEMBER_ID, REMAINING_USER_ID);
    }

    @Nested
    class WhenEventIsConsumed {

        @Test
        void shouldDelegateEventToSharedAccountDeletionService() {
            deletionConsumer.deleteDataFromSharedAccount(event);

            verify(sharedAccountDeletionService, times(1)).deleteData(event);
        }
    }
}