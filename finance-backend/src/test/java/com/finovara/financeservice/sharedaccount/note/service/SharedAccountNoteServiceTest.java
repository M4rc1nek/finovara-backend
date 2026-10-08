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
    private static final String USERNAME = "john";
    private static final String OTHER_USERNAME = "anna";
    private static final String AGGREGATE = "SharedAccountNote";
    private static final String TOPIC = "shared-account.activity";
    private static final String NOTE_TOPIC = "Groceries";
    private static final String NOTE_DESCRIPTION = "Buy milk and bread";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 10, 10, 0);

    @Mock
    private SharedAccountNoteRepository sharedAccountNoteRepository;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private AuthBackendClient authBackendClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SharedAccountNoteDto noteDto;

    @Mock
    private SharedAccountParticipantsResponse participantsResponse;

    @Mock
    private SharedAccountNote savedNote;

    @Mock
    private SharedAccountNote existingNote;

    @Captor
    private ArgumentCaptor<SharedAccountNote> noteCaptor;

    @Captor
    private ArgumentCaptor<SharedAccountActivityLogEvent> eventCaptor;

    @InjectMocks
    private SharedAccountNoteService service;

    private void stubParticipants(Long userId) {
        when(sharedAccountParticipantsService.getParticipants(userId)).thenReturn(participantsResponse);
        when(participantsResponse.ownerId()).thenReturn(OWNER_ID);
        when(participantsResponse.memberId()).thenReturn(MEMBER_ID);
    }

    private void verifyOutboxEvent(Long userId, SharedAccountActivityLogType type) {
        verify(outboxService).save(eq(AGGREGATE), eq(NOTE_ID.toString()), eq(TOPIC), eventCaptor.capture());
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

        private void stubCreateInputs() {
            stubParticipants(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(noteDto.topic()).thenReturn(NOTE_TOPIC);
            when(noteDto.description()).thenReturn(NOTE_DESCRIPTION);
        }

        private void stubSavedNote() {
            when(sharedAccountNoteRepository.save(any(SharedAccountNote.class))).thenReturn(savedNote);
            when(savedNote.getId()).thenReturn(NOTE_ID);
        }

        @Test
        void shouldReturnResponseWhenNoteIsCreated() {
            stubCreateInputs();
            stubSavedNote();

            SharedAccountNoteResponse result = service.createNote(OWNER_ID, noteDto);

            assertEquals(new SharedAccountNoteResponse(NOTE_ID, OWNER_ID, USERNAME), result);
        }

        @Test
        void shouldSaveNoteWithDtoAndParticipantDataWhenNoteIsCreated() {
            stubCreateInputs();
            stubSavedNote();

            service.createNote(OWNER_ID, noteDto);

            verify(sharedAccountNoteRepository).save(noteCaptor.capture());
            SharedAccountNote saved = noteCaptor.getValue();
            assertEquals(NOTE_TOPIC, saved.getTopic());
            assertEquals(NOTE_DESCRIPTION, saved.getDescription());
            assertEquals(OWNER_ID, saved.getOwnerId());
            assertEquals(MEMBER_ID, saved.getMemberId());
            assertEquals(OWNER_ID, saved.getCreatedByUserId());
            assertNotNull(saved.getCreatedAt());
        }

        @Test
        void shouldSaveNoteWithMemberAsCreatorWhenMemberCreatesNote() {
            stubParticipants(MEMBER_ID);
            when(authBackendClient.getUsername(MEMBER_ID)).thenReturn(OTHER_USERNAME);
            when(noteDto.topic()).thenReturn(NOTE_TOPIC);
            when(noteDto.description()).thenReturn(NOTE_DESCRIPTION);
            stubSavedNote();

            SharedAccountNoteResponse result = service.createNote(MEMBER_ID, noteDto);

            verify(sharedAccountNoteRepository).save(noteCaptor.capture());
            assertEquals(MEMBER_ID, noteCaptor.getValue().getCreatedByUserId());
            assertEquals(new SharedAccountNoteResponse(NOTE_ID, MEMBER_ID, OTHER_USERNAME), result);
        }

        @Test
        void shouldSaveOutboxEventWhenNoteIsCreated() {
            stubCreateInputs();
            stubSavedNote();

            service.createNote(OWNER_ID, noteDto);

            verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.NOTE_CREATED);
        }

        @Test
        void shouldSaveNoteBeforeOutboxEventWhenNoteIsCreated() {
            stubCreateInputs();
            stubSavedNote();

            service.createNote(OWNER_ID, noteDto);

            InOrder inOrder = inOrder(sharedAccountNoteRepository, outboxService);
            inOrder.verify(sharedAccountNoteRepository).save(any(SharedAccountNote.class));
            inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
        }

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
        void shouldThrowExceptionWhenRepositorySaveFails() {
            stubCreateInputs();
            when(sharedAccountNoteRepository.save(any(SharedAccountNote.class)))
                    .thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.createNote(OWNER_ID, noteDto));

            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenOutboxServiceFails() {
            stubCreateInputs();
            stubSavedNote();
            doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                    .save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class, () -> service.createNote(OWNER_ID, noteDto));
        }
    }

    @Nested
    class EditNote {

        private void stubEditInputs() {
            when(noteDto.topic()).thenReturn(NOTE_TOPIC);
            when(noteDto.description()).thenReturn(NOTE_DESCRIPTION);
        }

        private void stubSuccessfulEdit(Long userId) {
            when(sharedAccountNoteRepository.findById(NOTE_ID)).thenReturn(Optional.of(existingNote));
            when(existingNote.getOwnerId()).thenReturn(OWNER_ID);
            when(existingNote.getId()).thenReturn(NOTE_ID);
            when(sharedAccountNoteRepository.save(existingNote)).thenReturn(existingNote);
            stubEditInputs();
            stubParticipants(userId);
        }

        @Test
        void shouldReturnNoteIdWhenOwnerEditsNote() {
            stubSuccessfulEdit(OWNER_ID);

            Long result = service.editNote(OWNER_ID, NOTE_ID, noteDto);

            assertEquals(NOTE_ID, result);
        }

        @Test
        void shouldReturnNoteIdWhenMemberEditsNote() {
            when(sharedAccountNoteRepository.findById(NOTE_ID)).thenReturn(Optional.of(existingNote));
            when(existingNote.getOwnerId()).thenReturn(OWNER_ID);
            when(existingNote.getMemberId()).thenReturn(MEMBER_ID);
            when(existingNote.getId()).thenReturn(NOTE_ID);
            when(sharedAccountNoteRepository.save(existingNote)).thenReturn(existingNote);
            stubEditInputs();
            stubParticipants(MEMBER_ID);

            Long result = service.editNote(MEMBER_ID, NOTE_ID, noteDto);

            assertEquals(NOTE_ID, result);
            verifyOutboxEvent(MEMBER_ID, SharedAccountActivityLogType.NOTE_EDITED);
        }

        @Test
        void shouldUpdateNoteFieldsWhenNoteIsEdited() {
            stubSuccessfulEdit(OWNER_ID);

            service.editNote(OWNER_ID, NOTE_ID, noteDto);

            verify(existingNote).setTopic(NOTE_TOPIC);
            verify(existingNote).setDescription(NOTE_DESCRIPTION);
            verify(sharedAccountNoteRepository).save(existingNote);
        }

        @Test
        void shouldSaveOutboxEventWhenNoteIsEdited() {
            stubSuccessfulEdit(OWNER_ID);

            service.editNote(OWNER_ID, NOTE_ID, noteDto);

            verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.NOTE_EDITED);
        }

        @Test
        void shouldSaveNoteBeforeOutboxEventWhenNoteIsEdited() {
            stubSuccessfulEdit(OWNER_ID);

            service.editNote(OWNER_ID, NOTE_ID, noteDto);

            InOrder inOrder = inOrder(sharedAccountNoteRepository, outboxService);
            inOrder.verify(sharedAccountNoteRepository).save(existingNote);
            inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
        }

        @Test
        void shouldThrowExceptionWhenNoteDoesNotExist() {
            when(sharedAccountNoteRepository.findById(NOTE_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.editNote(OWNER_ID, NOTE_ID, noteDto));

            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenUserIsNeitherOwnerNorMember() {
            when(sharedAccountNoteRepository.findById(NOTE_ID)).thenReturn(Optional.of(existingNote));
            when(existingNote.getOwnerId()).thenReturn(OWNER_ID);
            when(existingNote.getMemberId()).thenReturn(MEMBER_ID);

            assertThrows(RequestedEntityNotFoundException.class, () -> service.editNote(OUTSIDER_ID, NOTE_ID, noteDto));

            verify(sharedAccountNoteRepository, never()).save(any(SharedAccountNote.class));
            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenDtoIsNull() {
            when(sharedAccountNoteRepository.findById(NOTE_ID)).thenReturn(Optional.of(existingNote));
            when(existingNote.getOwnerId()).thenReturn(OWNER_ID);

            assertThrows(NullPointerException.class, () -> service.editNote(OWNER_ID, NOTE_ID, null));

            verify(sharedAccountNoteRepository, never()).save(any(SharedAccountNote.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFindFails() {
            when(sharedAccountNoteRepository.findById(NOTE_ID)).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.editNote(OWNER_ID, NOTE_ID, noteDto));

            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenRepositorySaveFails() {
            when(sharedAccountNoteRepository.findById(NOTE_ID)).thenReturn(Optional.of(existingNote));
            when(existingNote.getOwnerId()).thenReturn(OWNER_ID);
            stubEditInputs();
            when(sharedAccountNoteRepository.save(existingNote)).thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> service.editNote(OWNER_ID, NOTE_ID, noteDto));

            verifyNoInteractions(sharedAccountParticipantsService, outboxService);
        }

        @Test
        void shouldThrowExceptionWhenOutboxServiceFails() {
            stubSuccessfulEdit(OWNER_ID);
            doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                    .save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class, () -> service.editNote(OWNER_ID, NOTE_ID, noteDto));
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
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingNote));
            when(existingNote.getId()).thenReturn(NOTE_ID);
            when(existingNote.getTopic()).thenReturn(NOTE_TOPIC);
            when(existingNote.getDescription()).thenReturn(NOTE_DESCRIPTION);
            when(existingNote.getCreatedAt()).thenReturn(CREATED_AT);
            when(existingNote.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);

            List<SharedAccountNoteDto> result = service.getNotes(OWNER_ID);

            assertEquals(List.of(new SharedAccountNoteDto(NOTE_ID, NOTE_TOPIC, NOTE_DESCRIPTION, CREATED_AT, OWNER_ID, USERNAME)), result);
        }

        @Test
        void shouldReturnNoteDtosWithCreatorUsernamesWhenCreatorsDiffer() {
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(OWNER_ID))
                    .thenReturn(List.of(existingNote, savedNote));
            when(existingNote.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(savedNote.getCreatedByUserId()).thenReturn(MEMBER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);
            when(authBackendClient.getUsername(MEMBER_ID)).thenReturn(OTHER_USERNAME);

            List<SharedAccountNoteDto> result = service.getNotes(OWNER_ID);

            assertEquals(2, result.size());
            assertEquals(new SharedAccountNoteDto(existingNote.getId(), existingNote.getTopic(), existingNote.getDescription(),
                    existingNote.getCreatedAt(), OWNER_ID, USERNAME), result.get(0));
            assertEquals(new SharedAccountNoteDto(savedNote.getId(), savedNote.getTopic(), savedNote.getDescription(),
                    savedNote.getCreatedAt(), MEMBER_ID, OTHER_USERNAME), result.get(1));
        }

        @Test
        void shouldFetchUsernameOnceWhenNotesShareSameCreator() {
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(OWNER_ID))
                    .thenReturn(List.of(existingNote, savedNote));
            when(existingNote.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(savedNote.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn(USERNAME);

            List<SharedAccountNoteDto> result = service.getNotes(OWNER_ID);

            assertEquals(2, result.size());
            verify(authBackendClient).getUsername(OWNER_ID);
        }

        @Test
        void shouldThrowExceptionWhenUsernameIsNull() {
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingNote));
            when(existingNote.getCreatedByUserId()).thenReturn(OWNER_ID);

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
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(OWNER_ID)).thenReturn(List.of(existingNote));
            when(existingNote.getCreatedByUserId()).thenReturn(OWNER_ID);
            when(authBackendClient.getUsername(OWNER_ID)).thenThrow(new IllegalStateException("auth down"));

            assertThrows(IllegalStateException.class, () -> service.getNotes(OWNER_ID));
        }
    }

    @Nested
    class DeleteNote {

        private void stubSuccessfulDelete() {
            when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID))
                    .thenReturn(Optional.of(existingNote));
            when(existingNote.getId()).thenReturn(NOTE_ID);
            stubParticipants(OWNER_ID);
        }

        @Test
        void shouldDeleteNoteWhenNoteExists() {
            stubSuccessfulDelete();

            service.deleteNote(OWNER_ID, NOTE_ID);

            verify(sharedAccountNoteRepository).delete(existingNote);
        }

        @Test
        void shouldSaveOutboxEventWhenNoteIsDeleted() {
            stubSuccessfulDelete();

            service.deleteNote(OWNER_ID, NOTE_ID);

            verifyOutboxEvent(OWNER_ID, SharedAccountActivityLogType.NOTE_DELETED);
        }

        @Test
        void shouldDeleteNoteBeforeSavingOutboxEventWhenNoteIsDeleted() {
            stubSuccessfulDelete();

            service.deleteNote(OWNER_ID, NOTE_ID);

            InOrder inOrder = inOrder(sharedAccountNoteRepository, outboxService);
            inOrder.verify(sharedAccountNoteRepository).delete(existingNote);
            inOrder.verify(outboxService).save(anyString(), anyString(), anyString(), any());
        }

        @Test
        void shouldThrowExceptionWhenNoteDoesNotExist() {
            when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, OWNER_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteNote(OWNER_ID, NOTE_ID));

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
                    .thenReturn(Optional.of(existingNote));
            when(sharedAccountParticipantsService.getParticipants(OWNER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("participants not found"));

            assertThrows(RequestedEntityNotFoundException.class, () -> service.deleteNote(OWNER_ID, NOTE_ID));

            verify(sharedAccountNoteRepository, never()).delete(any(SharedAccountNote.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldThrowExceptionWhenOutboxServiceFails() {
            stubSuccessfulDelete();
            doThrow(new IllegalStateException("outbox failed")).when(outboxService)
                    .save(anyString(), anyString(), anyString(), any());

            assertThrows(IllegalStateException.class, () -> service.deleteNote(OWNER_ID, NOTE_ID));
        }
    }
}