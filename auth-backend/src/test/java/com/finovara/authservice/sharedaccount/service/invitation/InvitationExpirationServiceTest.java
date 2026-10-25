package com.finovara.authservice.sharedaccount.service.invitation;

import com.finovara.authservice.sharedaccount.dto.ExpiredInvitationDto;
import com.finovara.authservice.sharedaccount.model.SharedAccountInvitation;
import com.finovara.authservice.sharedaccount.repository.SharedAccountInvitationRepository;
import com.finovara.contracts.outbox.OutboxService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationExpirationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Mock
    private SharedAccountInvitationRepository invitationRepository;
    @Mock
    private OutboxService outboxService;
    @InjectMocks
    private InvitationExpirationService service;

    @Nested
    class ExpireBatch {

        @Test
        void shouldReturnZeroWhenNoInvitationHasExpired() {
            when(invitationRepository.findExpired(eq(NOW), any())).thenReturn(List.of());
            int result = service.expireBatch(NOW, 10);

            assertEquals(0, result);
            verify(invitationRepository, never()).findByIds(any());
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldDeleteExpiredInvitationsAndPublishEventsWhenInvitationsExist() {
            ExpiredInvitationDto first = new ExpiredInvitationDto(1L, 10L, "anna");
            ExpiredInvitationDto second = new ExpiredInvitationDto(2L, 11L, "bob");
            List<SharedAccountInvitation> entities = List.of(new SharedAccountInvitation(), new SharedAccountInvitation());
            when(invitationRepository.findExpired(eq(NOW), any())).thenReturn(List.of(first, second));
            when(invitationRepository.findByIds(List.of(1L, 2L))).thenReturn(entities);

            int result = service.expireBatch(NOW, 2);

            assertEquals(2, result);
            verify(invitationRepository).deleteAll(entities);
            verify(outboxService).save(eq("User"), eq("10"), eq("notification.shared-account.invitation-expired"), any());
            verify(outboxService).save(eq("User"), eq("11"), eq("notification.shared-account.invitation-expired"), any());
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFailsToLoadExpiredInvitations() {
            when(invitationRepository.findExpired(eq(NOW), any())).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.expireBatch(NOW, 10));
            verifyNoInteractions(outboxService);
        }
    }
}
