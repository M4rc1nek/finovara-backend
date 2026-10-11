package com.finovara.activitylogservice.activitylog.sharedaccount.consumer;

import com.finovara.activitylogservice.activitylog.sharedaccount.repository.SharedAccountActivityLogRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SharedAccountActivityDataDeleterTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;

    @Mock
    private SharedAccountActivityLogRepository sharedAccountActivityLogRepository;

    @InjectMocks
    private SharedAccountActivityDataDeleter sharedAccountActivityDataDeleter;

    @Nested
    class DeleteSharedAccountActivityLogData {

        @Test
        void shouldDeleteActivityLogsWhenOwnerIdAndMemberIdAreProvided() {
            sharedAccountActivityDataDeleter.deleteSharedAccountActivityLogData(OWNER_ID, MEMBER_ID);

            verify(sharedAccountActivityLogRepository).deleteByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);
            verifyNoMoreInteractions(sharedAccountActivityLogRepository);
        }

        @Test
        void shouldDeleteActivityLogsWhenOwnerIdIsNull() {
            sharedAccountActivityDataDeleter.deleteSharedAccountActivityLogData(null, MEMBER_ID);

            verify(sharedAccountActivityLogRepository).deleteByOwnerIdAndMemberId(null, MEMBER_ID);
        }

        @Test
        void shouldDeleteActivityLogsWhenMemberIdIsNull() {
            sharedAccountActivityDataDeleter.deleteSharedAccountActivityLogData(OWNER_ID, null);

            verify(sharedAccountActivityLogRepository).deleteByOwnerIdAndMemberId(OWNER_ID, null);
        }

        @Test
        void shouldDeleteActivityLogsWhenOwnerIdAndMemberIdAreNull() {
            sharedAccountActivityDataDeleter.deleteSharedAccountActivityLogData(null, null);

            verify(sharedAccountActivityLogRepository).deleteByOwnerIdAndMemberId(null, null);
        }

        @Test
        void shouldDeleteActivityLogsWhenOwnerIdEqualsMemberId() {
            sharedAccountActivityDataDeleter.deleteSharedAccountActivityLogData(OWNER_ID, OWNER_ID);

            verify(sharedAccountActivityLogRepository).deleteByOwnerIdAndMemberId(OWNER_ID, OWNER_ID);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryDeleteFails() {
            doThrow(new IllegalStateException()).when(sharedAccountActivityLogRepository)
                    .deleteByOwnerIdAndMemberId(OWNER_ID, MEMBER_ID);

            assertThrows(IllegalStateException.class,
                    () -> sharedAccountActivityDataDeleter.deleteSharedAccountActivityLogData(OWNER_ID, MEMBER_ID));
        }
    }
}