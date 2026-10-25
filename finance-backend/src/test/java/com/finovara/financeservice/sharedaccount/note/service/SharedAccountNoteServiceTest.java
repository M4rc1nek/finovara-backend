package com.finovara.financeservice.sharedaccount.note.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.financeservice.sharedaccount.note.dto.SharedAccountNoteDto;
import com.finovara.financeservice.sharedaccount.note.dto.SharedAccountNoteResponse;
import com.finovara.financeservice.sharedaccount.note.model.SharedAccountNote;
import com.finovara.financeservice.sharedaccount.note.repository.SharedAccountNoteRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.util.sharedaccount.SharedUsernameResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedAccountNoteServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 3L;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long NOTE_ID = 5L;
    private static final String USERNAME = "john";
    private static final String OTHER_USERNAME = "anna";
    private static final String TOPIC = "Groceries";
    private static final String DESCRIPTION = "Buy milk and bread";
    private static final String OUTBOX_AGGREGATE = "SharedAccountNote";
    private static final String OUTBOX_TOPIC = "shared-account.activity";
    private static final int PAGE_SIZE = 10;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 1, 12, 0);

    @Mock
    private SharedAccountNoteRepository sharedAccountNoteRepository;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private SharedUsernameResolver usernameResolver;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SharedAccountParticipantsResponse participants;

    @Captor
    private ArgumentCaptor<SharedAccountNote> noteCaptor;

    @InjectMocks
    private SharedAccountNoteService service;

    private SharedAccountNoteDto noteDto;
    private SharedAccountNote existingNote;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", PAGE_SIZE);
        noteDto = new SharedAccountNoteDto(null, TOPIC, DESCRIPTION, null, null, null);
        existingNote = SharedAccountNote.builder()
                .id(NOTE_ID)
                .topic("Old topic")
                .description("Old description")
                .createdAt(CREATED_AT)
                .ownerId(OWNER_ID)
                .memberId(MEMBER_ID)
                .createdByUserId(USER_ID)
                .build();
    }

    @Nested
    class CreateNote {

        @Nested
        class Persisted {

            @BeforeEach
            void setUp() {
                when(participants.ownerId()).thenReturn(OWNER_ID);
                when(participants.memberId()).thenReturn(MEMBER_ID);
                when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);
                when(sharedAccountNoteRepository.save(any(SharedAccountNote.class))).thenReturn(existingNote);
            }

            @Test
            void shouldReturnResponseWithIdAndUsernameWhenNoteCreated() {
                when(usernameResolver.resolve(List.of(USER_ID))).thenReturn(Map.of(USER_ID, USERNAME));

                SharedAccountNoteResponse result = service.createNote(USER_ID, noteDto);

                assertEquals(new SharedAccountNoteResponse(NOTE_ID, USER_ID, USERNAME), result);
            }

            @Test
            void shouldReturnResponseWithNullUsernameWhenUsernameNotResolved() {
                when(usernameResolver.resolve(List.of(USER_ID))).thenReturn(Map.of());

                SharedAccountNoteResponse result = service.createNote(USER_ID, noteDto);

                assertEquals(new SharedAccountNoteResponse(NOTE_ID, USER_ID, null), result);
            }

            @Test
            void shouldSaveNoteWithParticipantsAndCreatorWhenNoteCreated() {
                when(usernameResolver.resolve(List.of(USER_ID))).thenReturn(Map.of(USER_ID, USERNAME));

                service.createNote(USER_ID, noteDto);

                verify(sharedAccountNoteRepository).save(noteCaptor.capture());
                assertEquals(TOPIC, noteCaptor.getValue().getTopic());
                assertEquals(DESCRIPTION, noteCaptor.getValue().getDescription());
                assertEquals(OWNER_ID, noteCaptor.getValue().getOwnerId());
                assertEquals(MEMBER_ID, noteCaptor.getValue().getMemberId());
                assertEquals(USER_ID, noteCaptor.getValue().getCreatedByUserId());
                assertTrue(noteCaptor.getValue().getCreatedAt() != null);
            }

            @Test
            void shouldLogActivityWhenNoteCreated() {
                when(usernameResolver.resolve(List.of(USER_ID))).thenReturn(Map.of(USER_ID, USERNAME));

                service.createNote(USER_ID, noteDto);

                verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(NOTE_ID.toString()), eq(OUTBOX_TOPIC), any(SharedAccountActivityLogEvent.class));
            }
        }

        @Test
        void shouldThrowExceptionWhenParticipantsNotFound() {
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.createNote(USER_ID, noteDto));
            verifyNoInteractions(usernameResolver, sharedAccountNoteRepository, outboxService);
        }
    }

    @Nested
    class EditNote {

        @Test
        void shouldUpdateNoteAndReturnIdWhenNoteExists() {
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);
            when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, USER_ID)).thenReturn(Optional.of(existingNote));
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);

            Long result = service.editNote(USER_ID, NOTE_ID, noteDto);

            assertEquals(NOTE_ID, result);
            assertEquals(TOPIC, existingNote.getTopic());
            assertEquals(DESCRIPTION, existingNote.getDescription());
        }

        @Test
        void shouldLogActivityWhenNoteEdited() {
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);
            when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, USER_ID)).thenReturn(Optional.of(existingNote));
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);

            service.editNote(USER_ID, NOTE_ID, noteDto);

            verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(NOTE_ID.toString()), eq(OUTBOX_TOPIC), any(SharedAccountActivityLogEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenNoteNotFound() {
            when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.editNote(USER_ID, NOTE_ID, noteDto));
            assertEquals("Old topic", existingNote.getTopic());
            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }
    }

    @Nested
    class GetNotes {

        @Test
        void shouldReturnNotesWithUsernamesWhenNotesExist() {
            SharedAccountNote otherNote = SharedAccountNote.builder()
                    .id(6L)
                    .topic("Bills")
                    .description("Pay electricity")
                    .createdAt(CREATED_AT)
                    .createdByUserId(OTHER_USER_ID)
                    .build();
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(USER_ID, PageRequest.of(1, PAGE_SIZE))).thenReturn(List.of(existingNote, otherNote));
            when(usernameResolver.resolve(Set.of(USER_ID, OTHER_USER_ID))).thenReturn(Map.of(USER_ID, USERNAME, OTHER_USER_ID, OTHER_USERNAME));

            List<SharedAccountNoteDto> result = service.getNotes(USER_ID, 1);

            assertEquals(List.of(
                    new SharedAccountNoteDto(NOTE_ID, "Old topic", "Old description", CREATED_AT, USER_ID, USERNAME),
                    new SharedAccountNoteDto(6L, "Bills", "Pay electricity", CREATED_AT, OTHER_USER_ID, OTHER_USERNAME)), result);
        }

        @Test
        void shouldReturnNullUsernameWhenUsernameNotResolved() {
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(USER_ID, PageRequest.of(0, PAGE_SIZE))).thenReturn(List.of(existingNote));
            when(usernameResolver.resolve(Set.of(USER_ID))).thenReturn(Map.of());

            List<SharedAccountNoteDto> result = service.getNotes(USER_ID, 0);

            assertEquals(List.of(new SharedAccountNoteDto(NOTE_ID, "Old topic", "Old description", CREATED_AT, USER_ID, null)), result);
        }

        @Test
        void shouldReturnEmptyListWhenNoNotesExist() {
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(USER_ID, PageRequest.of(0, PAGE_SIZE))).thenReturn(List.of());

            List<SharedAccountNoteDto> result = service.getNotes(USER_ID, 0);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class DeleteNote {

        @Test
        void shouldDeleteNoteAndLogActivityWhenNoteExists() {
            when(participants.ownerId()).thenReturn(OWNER_ID);
            when(participants.memberId()).thenReturn(MEMBER_ID);
            when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, USER_ID)).thenReturn(Optional.of(existingNote));
            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participants);

            service.deleteNote(USER_ID, NOTE_ID);

            verify(sharedAccountNoteRepository).delete(existingNote);
            verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq(NOTE_ID.toString()), eq(OUTBOX_TOPIC), any(SharedAccountActivityLogEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenNoteNotFound() {
            when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, USER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteNote(USER_ID, NOTE_ID));
            verify(sharedAccountNoteRepository, never()).delete(any(SharedAccountNote.class));
            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }
    }
}