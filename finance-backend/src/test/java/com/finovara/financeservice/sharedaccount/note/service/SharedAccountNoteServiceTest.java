package com.finovara.financeservice.sharedaccount.note.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.sharedaccount.note.dto.SharedAccountNoteDto;
import com.finovara.financeservice.sharedaccount.note.dto.SharedAccountNoteResponse;
import com.finovara.financeservice.sharedaccount.note.model.SharedAccountNote;
import com.finovara.financeservice.sharedaccount.note.repository.SharedAccountNoteRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SharedAccountNoteServiceTest {

    private static final Long OWNER_ID = 10L;
    private static final Long MEMBER_ID = 20L;
    private static final Long OUTSIDER_ID = 99L;
    private static final Long NOTE_ID = 100L;
    private static final Long OTHER_NOTE_ID = 200L;
    private static final String USERNAME = "john";
    private static final String OTHER_USERNAME = "anna";
    private static final String AGGREGATE = "SharedAccountNote";
    private static final String ACTIVITY_TOPIC = "shared-account.activity";
    private static final String NOTE_TOPIC = "Groceries";
    private static final String NOTE_DESCRIPTION = "Buy milk and bread";
    private static final String OTHER_NOTE_TOPIC = "Bills";
    private static final String OTHER_NOTE_DESCRIPTION = "Pay electricity";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 10, 10, 0);
    private static final LocalDateTime OTHER_CREATED_AT = LocalDateTime.of(2026, 1, 11, 12, 30);

    @Mock
    private SharedAccountNoteRepository sharedAccountNoteRepository;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SharedAccountParticipantsResponse participantsResponse;

    @Mock
    private SharedAccountNote note;

    @Mock
    private SharedAccountNote otherNote;

    @Mock
    private SharedAccountNote savedNote;

    @Captor
    private ArgumentCaptor<SharedAccountNote> noteCaptor;

    @Captor
    private ArgumentCaptor<SharedAccountActivityLogEvent> eventCaptor;

    @InjectMocks
    private SharedAccountNoteService service;

    private SharedAccountNoteDto noteDto;

    @BeforeEach
    void setUp() {
        noteDto = new SharedAccountNoteDto(NOTE_ID, NOTE_TOPIC, NOTE_DESCRIPTION, CREATED_AT, OWNER_ID, USERNAME);
    }

    private void stubParticipants(Long userId) {
        when(sharedAccountParticipantsService.getParticipants(userId)).thenReturn(participantsResponse);
        when(participantsResponse.ownerId()).thenReturn(OWNER_ID);
        when(participantsResponse.memberId()).thenReturn(MEMBER_ID);
    }

    private void stubNote(SharedAccountNote mockNote, Long id, String topic, String description, LocalDateTime createdAt, Long creatorId) {
        when(mockNote.getId()).thenReturn(id);
        when(mockNote.getTopic()).thenReturn(topic);
        when(mockNote.getDescription()).thenReturn(description);
        when(mockNote.getCreatedAt()).thenReturn(createdAt);
        when(mockNote.getCreatedByUserId()).thenReturn(creatorId);
    }

    private void verifyOutboxEvent(Long userId, SharedAccountActivityLogType type) {
        verify(outboxService).save(eq(AGGREGATE), eq(NOTE_ID.toString()), eq(ACTIVITY_TOPIC), eventCaptor.capture());
        SharedAccountActivityLogEvent event = eventCaptor.getValue();
        assertEquals(OWNER_ID, event.ownerId());
        assertEquals(MEMBER_ID, event.memberId());
        assertEquals(userId, event.userId());
        assertEquals(NOTE_ID, event.targetId());
        assertEquals(type, event.type());
        assertNotNull(event.createdAt());
    }

    @Nested
    class CreateNote {

        @Nested
        class Success {

            @BeforeEach
            void setUp() {
                stubParticipants(OWNER_ID);
                when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
                when(sharedAccountNoteRepository.save(any(SharedAccountNote.class))).thenReturn(savedNote);
                when(savedNote.getId()).thenReturn(NOTE_ID);
            }

            @Test
            void shouldReturnResponseWhenNoteIsCreated() {
                SharedAccountNoteResponse result = service.createNote(OWNER_ID, noteDto);

                assertEquals(new SharedAccountNoteResponse(NOTE_ID, OWNER_ID, USERNAME), result);
            }

            @Test
            void shouldSaveNoteWithDtoAndParticipantDataWhenNoteIsCreated() {
                service.createNote(OWNER_ID, noteDto);

                verify(sharedAccountNoteRepository).save(noteCaptor.capture());
                SharedAccountNote captured = noteCaptor.getValue();
                assertEquals(NOTE_TOPIC, captured.getTopic());
                assertEquals(NOTE_DESCRIPTION, captured.getDescription());
                assertEquals(OWNER_ID, captured.getOwnerId());
                assertEquals(MEMBER_ID, captured.getMemberId());
                assertEquals(OWNER_ID, captured.getCreatedByUserId());
                assertNotNull(captured.getCreatedAt());
            }

            @Test
            void shouldSaveOutboxEventWhenNoteIsCreated() {
                service.createNote(OWNER_ID, noteDto);

                verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.NOTE_CREATED);
            }

            @Test
            void shouldSaveNoteBeforeOutboxEventWhenNoteIsCreated() {
                service.createNote(OWNER_ID, noteDto);

                InOrder inOrder = inOrder(sharedAccountNoteRepository, outboxService);
                inOrder.verify(sharedAccountNoteRepository).save(any(SharedAccountNote.class));
                inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
            }

            @Test
            void shouldFetchParticipantsAndUsernameOnceWhenNoteIsCreated() {
                service.createNote(OWNER_ID, noteDto);

                verify(sharedAccountParticipantsService).getParticipants(OWNER_ID);
                verify(authBackendClient).getUsername(OWNER_ID);
            }
        }

        @Nested
        class Failure {

            @Test
            void shouldThrowExceptionWhenDtoIsNull() {
                assertThrows(NullPointerException.class, () -> service.createNote(OWNER_ID, null));

                verifyNoInteractions(sharedAccountNoteRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenParticipantsResponseIsNull() {
                assertThrows(NullPointerException.class, () -> service.createNote(OWNER_ID, noteDto));

                verifyNoInteractions(sharedAccountNoteRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenParticipantsServiceFails() {
                when(sharedAccountParticipantsService.getParticipants(OWNER_ID))
                        .thenThrow(new RequestedEntityNotFoundException("participants not found"));

                assertThrows(RequestedEntityNotFoundException.class, () -> service.createNote(OWNER_ID, noteDto));

                verifyNoInteractions(authBackendClient, sharedAccountNoteRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenAuthBackendClientFails() {
                when(sharedAccountParticipantsService.getParticipants(OWNER_ID)).thenReturn(participantsResponse);
                when(authBackendClient.getUsername(OWNER_ID)).thenThrow(new IllegalStateException("auth down"));

                assertThrows(IllegalStateException.class, () -> service.createNote(OWNER_ID, noteDto));

                verifyNoInteractions(sharedAccountNoteRepository, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenRepositorySaveFails() {
                stubParticipants(OWNER_ID);
                when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
                when(sharedAccountNoteRepository.save(any(SharedAccountNote.class)))
                        .thenThrow(new IllegalStateException("db failed"));

                assertThrows(IllegalStateException.class, () -> service.createNote(OWNER_ID, noteDto));

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenOutboxServiceFails() {
                stubParticipants(OWNER_ID);
                when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
                when(sharedAccountNoteRepository.save(any(SharedAccountNote.class))).thenReturn(savedNote);
                when(savedNote.getId()).thenReturn(NOTE_ID);
                doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                        .save(anyString(), anyString(), anyString(), any());

                assertThrows(IllegalStateException.class, () -> service.createNote(OWNER_ID, noteDto));
            }
        }

        @Test
        void shouldSaveNoteWithMemberAsCreatorWhenMemberCreatesNote() {
            stubParticipants(MEMBER_ID);
            when(authBackendClient.getUsername(MEMBER_ID)).thenReturn(OTHER_USERNAME);
            when(sharedAccountNoteRepository.save(any(SharedAccountNote.class))).thenReturn(savedNote);
            when(savedNote.getId()).thenReturn(NOTE_ID);

            SharedAccountNoteResponse result = service.createNote(MEMBER_ID, noteDto);

            verify(sharedAccountNoteRepository).save(noteCaptor.capture());
            assertEquals(MEMBER_ID, noteCaptor.getValue().getCreatedByUserId());
            assertEquals(new SharedAccountNoteResponse(NOTE_ID, MEMBER_ID, OTHER_USERNAME), result);
            verifyOutboxEvent(MEMBER_ID, SharedAccountActivityLogType.NOTE_CREATED);
        }

        @Test
        void shouldReturnResponseWithNullUsernameWhenAuthBackendReturnsNull() {
            stubParticipants(OWNER_ID);
            when(sharedAccountNoteRepository.save(any(SharedAccountNote.class))).thenReturn(savedNote);
            when(savedNote.getId()).thenReturn(NOTE_ID);

            SharedAccountNoteResponse result = service.createNote(OWNER_ID, noteDto);

            assertEquals(new SharedAccountNoteResponse(NOTE_ID, OWNER_ID, null), result);
        }
    }

    @Nested
    class EditNote {

        @Nested
        class Success {

            @BeforeEach
            void setUp() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenReturn(Optional.of(note));
                when(sharedAccountNoteRepository.save(note)).thenReturn(savedNote);
                when(savedNote.getId()).thenReturn(NOTE_ID);
                stubParticipants(OWNER_ID);
            }

            @Test
            void shouldReturnNoteIdWhenOwnerEditsNote() {
                Long result = service.editNote(OWNER_ID, NOTE_ID, noteDto);

                assertEquals(NOTE_ID, result);
            }

            @Test
            void shouldUpdateNoteFieldsWhenNoteIsEdited() {
                service.editNote(OWNER_ID, NOTE_ID, noteDto);

                verify(note).setTopic(NOTE_TOPIC);
                verify(note).setDescription(NOTE_DESCRIPTION);
                verify(sharedAccountNoteRepository).save(note);
            }

            @Test
            void shouldSaveOutboxEventWhenNoteIsEdited() {
                service.editNote(OWNER_ID, NOTE_ID, noteDto);

                verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.NOTE_EDITED);
            }

            @Test
            void shouldSaveNoteBeforeOutboxEventWhenNoteIsEdited() {
                service.editNote(OWNER_ID, NOTE_ID, noteDto);

                InOrder inOrder = inOrder(sharedAccountNoteRepository, outboxService);
                inOrder.verify(sharedAccountNoteRepository).save(note);
                inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
            }

            @Test
            void shouldNotFetchUsernameWhenNoteIsEdited() {
                service.editNote(OWNER_ID, NOTE_ID, noteDto);

                verifyNoInteractions(authBackendClient);
            }
        }

        @Nested
        class Failure {

            @Test
            void shouldThrowExceptionWhenNoteDoesNotExist() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenReturn(Optional.empty());

                assertThrows(RequestedEntityNotFoundException.class, () -> service.editNote(OWNER_ID, NOTE_ID, noteDto));

                verify(sharedAccountNoteRepository, never()).save(any(SharedAccountNote.class));
                verifyNoInteractions(sharedAccountParticipantsService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenUserIsNeitherOwnerNorMember() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OUTSIDER_ID))
                        .thenReturn(Optional.empty());

                assertThrows(RequestedEntityNotFoundException.class, () -> service.editNote(OUTSIDER_ID, NOTE_ID, noteDto));

                verify(sharedAccountNoteRepository, never()).save(any(SharedAccountNote.class));
                verifyNoInteractions(sharedAccountParticipantsService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenDtoIsNull() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenReturn(Optional.of(note));

                assertThrows(NullPointerException.class, () -> service.editNote(OWNER_ID, NOTE_ID, null));

                verifyNoInteractions(note, sharedAccountParticipantsService, outboxService);
                verify(sharedAccountNoteRepository, never()).save(any(SharedAccountNote.class));
            }

            @Test
            void shouldThrowExceptionWhenRepositoryLookupFails() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenThrow(new IllegalStateException("db failed"));

                assertThrows(IllegalStateException.class, () -> service.editNote(OWNER_ID, NOTE_ID, noteDto));

                verifyNoInteractions(sharedAccountParticipantsService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenRepositorySaveFails() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenReturn(Optional.of(note));
                when(sharedAccountNoteRepository.save(note)).thenThrow(new IllegalStateException("db failed"));

                assertThrows(IllegalStateException.class, () -> service.editNote(OWNER_ID, NOTE_ID, noteDto));

                verifyNoInteractions(sharedAccountParticipantsService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenParticipantsServiceFails() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenReturn(Optional.of(note));
                when(sharedAccountNoteRepository.save(note)).thenReturn(savedNote);
                when(sharedAccountParticipantsService.getParticipants(OWNER_ID))
                        .thenThrow(new RequestedEntityNotFoundException("participants not found"));

                assertThrows(RequestedEntityNotFoundException.class, () -> service.editNote(OWNER_ID, NOTE_ID, noteDto));

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenParticipantsResponseIsNull() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenReturn(Optional.of(note));
                when(sharedAccountNoteRepository.save(note)).thenReturn(savedNote);
                when(savedNote.getId()).thenReturn(NOTE_ID);

                assertThrows(NullPointerException.class, () -> service.editNote(OWNER_ID, NOTE_ID, noteDto));

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenOutboxServiceFails() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenReturn(Optional.of(note));
                when(sharedAccountNoteRepository.save(note)).thenReturn(savedNote);
                when(savedNote.getId()).thenReturn(NOTE_ID);
                stubParticipants(OWNER_ID);
                doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                        .save(anyString(), anyString(), anyString(), any());

                assertThrows(IllegalStateException.class, () -> service.editNote(OWNER_ID, NOTE_ID, noteDto));
            }
        }

        @Test
        void shouldReturnNoteIdAndSaveEventWhenMemberEditsNote() {
            when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, MEMBER_ID))
                    .thenReturn(Optional.of(note));
            when(sharedAccountNoteRepository.save(note)).thenReturn(savedNote);
            when(savedNote.getId()).thenReturn(NOTE_ID);
            stubParticipants(MEMBER_ID);

            Long result = service.editNote(MEMBER_ID, NOTE_ID, noteDto);

            assertEquals(NOTE_ID, result);
            verifyOutboxEvent(MEMBER_ID, SharedAccountActivityLogType.NOTE_EDITED);
        }
    }

    @Nested
    class GetNotes {

        @Test
        void shouldReturnEmptyListWhenNoNotesExist() {
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of());

            List<SharedAccountNoteDto> result = service.getNotes(OWNER_ID);

            assertTrue(result.isEmpty());
            verifyNoInteractions(authBackendClient);
        }

        @Test
        void shouldReturnNoteDtoWithUsernameWhenSingleNoteExists() {
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(note));
            stubNote(note, NOTE_ID, NOTE_TOPIC, NOTE_DESCRIPTION, CREATED_AT, OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);

            List<SharedAccountNoteDto> result = service.getNotes(OWNER_ID);

            assertEquals(List.of(noteDto), result);
        }

        @Test
        void shouldReturnNoteDtosWithCreatorUsernamesWhenCreatorsDiffer() {
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(OWNER_ID))
                    .thenReturn(List.of(note, otherNote));
            stubNote(note, NOTE_ID, NOTE_TOPIC, NOTE_DESCRIPTION, CREATED_AT, OWNER_ID);
            stubNote(otherNote, OTHER_NOTE_ID, OTHER_NOTE_TOPIC, OTHER_NOTE_DESCRIPTION, OTHER_CREATED_AT, MEMBER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(authBackendClient.getUsername(MEMBER_ID)).thenReturn(OTHER_USERNAME);

            List<SharedAccountNoteDto> result = service.getNotes(OWNER_ID);

            assertEquals(2, result.size());
            assertEquals(noteDto, result.get(0));
            assertEquals(new SharedAccountNoteDto(OTHER_NOTE_ID, OTHER_NOTE_TOPIC, OTHER_NOTE_DESCRIPTION,
                    OTHER_CREATED_AT, MEMBER_ID, OTHER_USERNAME), result.get(1));
        }

        @Test
        void shouldPreserveRepositoryOrderWhenMultipleNotesExist() {
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(OWNER_ID))
                    .thenReturn(List.of(otherNote, note));
            stubNote(note, NOTE_ID, NOTE_TOPIC, NOTE_DESCRIPTION, CREATED_AT, OWNER_ID);
            stubNote(otherNote, OTHER_NOTE_ID, OTHER_NOTE_TOPIC, OTHER_NOTE_DESCRIPTION, OTHER_CREATED_AT, MEMBER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(authBackendClient.getUsername(MEMBER_ID)).thenReturn(OTHER_USERNAME);

            List<SharedAccountNoteDto> result = service.getNotes(OWNER_ID);

            assertEquals(OTHER_NOTE_ID, result.get(0).id());
            assertEquals(NOTE_ID, result.get(1).id());
        }

        @Test
        void shouldFetchUsernameOnceWhenNotesShareSameCreator() {
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(OWNER_ID))
                    .thenReturn(List.of(note, otherNote));
            stubNote(note, NOTE_ID, NOTE_TOPIC, NOTE_DESCRIPTION, CREATED_AT, OWNER_ID);
            stubNote(otherNote, OTHER_NOTE_ID, OTHER_NOTE_TOPIC, OTHER_NOTE_DESCRIPTION, OTHER_CREATED_AT, OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);

            List<SharedAccountNoteDto> result = service.getNotes(OWNER_ID);

            assertEquals(2, result.size());
            verify(authBackendClient).getUsername(OWNER_ID);
        }

        @Test
        void shouldThrowExceptionWhenUsernameIsNull() {
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(note));
            when(note.getCreatedByUserId()).thenReturn(OWNER_ID);

            assertThrows(NullPointerException.class, () -> service.getNotes(OWNER_ID));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(OWNER_ID))
                    .thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.getNotes(OWNER_ID));

            verifyNoInteractions(authBackendClient);
        }

        @Test
        void shouldThrowExceptionWhenAuthBackendClientFails() {
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(note));
            when(note.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenThrow(new IllegalStateException("auth down"));

            assertThrows(IllegalStateException.class, () -> service.getNotes(OWNER_ID));
        }
    }

    @Nested
    class DeleteNote {

        @Nested
        class Success {

            @BeforeEach
            void setUp() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenReturn(Optional.of(note));
                when(note.getId()).thenReturn(NOTE_ID);
                stubParticipants(OWNER_ID);
            }

            @Test
            void shouldDeleteNoteWhenNoteExists() {
                service.deleteNote(OWNER_ID, NOTE_ID);

                verify(sharedAccountNoteRepository).delete(note);
            }

            @Test
            void shouldSaveOutboxEventWhenNoteIsDeleted() {
                service.deleteNote(OWNER_ID, NOTE_ID);

                verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.NOTE_DELETED);
            }

            @Test
            void shouldDeleteNoteBeforeSavingOutboxEventWhenNoteIsDeleted() {
                service.deleteNote(OWNER_ID, NOTE_ID);

                InOrder inOrder = inOrder(sharedAccountNoteRepository, outboxService);
                inOrder.verify(sharedAccountNoteRepository).delete(note);
                inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
            }

            @Test
            void shouldFetchParticipantsBeforeDeletingNoteWhenNoteIsDeleted() {
                service.deleteNote(OWNER_ID, NOTE_ID);

                InOrder inOrder = inOrder(sharedAccountParticipantsService, sharedAccountNoteRepository);
                inOrder.verify(sharedAccountParticipantsService).getParticipants(OWNER_ID);
                inOrder.verify(sharedAccountNoteRepository).delete(note);
            }

            @Test
            void shouldNotFetchUsernameWhenNoteIsDeleted() {
                service.deleteNote(OWNER_ID, NOTE_ID);

                verifyNoInteractions(authBackendClient);
            }
        }

        @Nested
        class Failure {

            @Test
            void shouldThrowExceptionWhenNoteDoesNotExist() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenReturn(Optional.empty());

                assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteNote(OWNER_ID, NOTE_ID));

                verify(sharedAccountNoteRepository, never()).delete(any(SharedAccountNote.class));
                verifyNoInteractions(sharedAccountParticipantsService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenUserIsNeitherOwnerNorMember() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OUTSIDER_ID))
                        .thenReturn(Optional.empty());

                assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteNote(OUTSIDER_ID, NOTE_ID));

                verify(sharedAccountNoteRepository, never()).delete(any(SharedAccountNote.class));
                verifyNoInteractions(sharedAccountParticipantsService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenRepositoryLookupFails() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenThrow(new IllegalStateException("db failed"));

                assertThrows(IllegalStateException.class, () -> service.deleteNote(OWNER_ID, NOTE_ID));

                verifyNoInteractions(sharedAccountParticipantsService, outboxService);
            }

            @Test
            void shouldThrowExceptionWhenParticipantsServiceFails() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenReturn(Optional.of(note));
                when(sharedAccountParticipantsService.getParticipants(OWNER_ID))
                        .thenThrow(new RequestedEntityNotFoundException("participants not found"));

                assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteNote(OWNER_ID, NOTE_ID));

                verify(sharedAccountNoteRepository, never()).delete(any(SharedAccountNote.class));
                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenParticipantsResponseIsNull() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenReturn(Optional.of(note));
                when(note.getId()).thenReturn(NOTE_ID);

                assertThrows(NullPointerException.class, () -> service.deleteNote(OWNER_ID, NOTE_ID));

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenRepositoryDeleteFails() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenReturn(Optional.of(note));
                when(sharedAccountParticipantsService.getParticipants(OWNER_ID)).thenReturn(participantsResponse);
                doThrow(new IllegalStateException("db failed")).when(sharedAccountNoteRepository).delete(note);

                assertThrows(IllegalStateException.class, () -> service.deleteNote(OWNER_ID, NOTE_ID));

                verifyNoInteractions(outboxService);
            }

            @Test
            void shouldThrowExceptionWhenOutboxServiceFails() {
                when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                        .thenReturn(Optional.of(note));
                when(note.getId()).thenReturn(NOTE_ID);
                stubParticipants(OWNER_ID);
                doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                        .save(anyString(), anyString(), anyString(), any());

                assertThrows(IllegalStateException.class, () -> service.deleteNote(OWNER_ID, NOTE_ID));
            }
        }

        @Test
        void shouldDeleteNoteAndSaveEventWhenMemberDeletesNote() {
            when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, MEMBER_ID))
                    .thenReturn(Optional.of(note));
            when(note.getId()).thenReturn(NOTE_ID);
            stubParticipants(MEMBER_ID);

            service.deleteNote(MEMBER_ID, NOTE_ID);

            verify(sharedAccountNoteRepository).delete(note);
            verifyOutboxEvent(MEMBER_ID, SharedAccountActivityLogType.NOTE_DELETED);
        }
    }
}