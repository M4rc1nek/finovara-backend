/*
package com.finovara.financeservice.sharedaccount.consumer;

import com.finovara.contracts.event.notification.sharedaccount.deletion.SharedAccountDeletedEvent;
import com.finovara.financeservice.sharedaccount.deletion.SharedAccountDeletionFinanceDataService;
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
    private SharedAccountDeletionFinanceDataService sharedAccountDeletionFinanceDataService;

    private DeletionConsumer deletionConsumer;

    private SharedAccountDeletedEvent event;

    @BeforeEach
    void setUp() {
        deletionConsumer = new DeletionConsumer(sharedAccountDeletionFinanceDataService);
        event = new SharedAccountDeletedEvent(ACCOUNT_ID, OWNER_ID, MEMBER_ID, REMAINING_USER_ID,
                "John", "john@gmail.com", "Adam", "adam@gmail.com");
    }

    @Nested
    class WhenEventIsConsumed {

        @Test
        void shouldDelegateEventToSharedAccountDeletionService() {
            deletionConsumer.deleteDataFromSharedAccount(event);

            verify(sharedAccountDeletionFinanceDataService, times(1)).deleteData(event);
        }
    }
}*/
