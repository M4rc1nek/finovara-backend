package com.finovara.financeservice.sharedaccount.note.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.sharedaccount.note.dto.SharedAccountNoteDto;
import com.finovara.financeservice.sharedaccount.note.dto.SharedAccountNoteResponse;
import com.finovara.financeservice.sharedaccount.note.model.SharedAccountNote;
import com.finovara.financeservice.sharedaccount.note.repository.SharedAccountNoteRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharedAccountNoteServiceTest {

    @Mock
    private SharedAccountNoteRepository sharedAccountNoteRepository;

    @Mock
    private SharedAccountParticipantsService sharedAccountParticipantsService;

    @Mock
    private AuthBackendClient authBackendClient;

    private SharedAccountNoteService sharedAccountNoteService;

    private static final Long USER_ID = 1L;
    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long NOTE_ID = 10L;
    private static final String USERNAME = "john_doe";
    private static final String TOPIC = "Go to the gym";
    private static final String DESCRIPTION = "Do a lot of chest exercises";

    @BeforeEach
    void setUp() {
        sharedAccountNoteService = new SharedAccountNoteService(
                sharedAccountNoteRepository,
                sharedAccountParticipantsService,
                authBackendClient
        );
    }

    private SharedAccountNote buildNote(Long id, Long ownerId, Long memberId, Long createdByUserId) {
        return SharedAccountNote.builder()
                .id(id)
                .topic(TOPIC)
                .description(DESCRIPTION)
                .createdAt(LocalDateTime.now())
                .ownerId(ownerId)
                .memberId(memberId)
                .createdByUserId(createdByUserId)
                .build();
    }

    private SharedAccountNoteDto buildNoteDto() {
        return new SharedAccountNoteDto(null, TOPIC, DESCRIPTION, null, null, null);
    }

    @Nested
    class CreateNote {

        @Test
        void shouldCreateNoteAndReturnResponseWhenValidDataProvided() {
            SharedAccountNoteDto requestDto = buildNoteDto();
            SharedAccountParticipantsResponse participantsResponse =
                    new SharedAccountParticipantsResponse(OWNER_ID, MEMBER_ID);

            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
            when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);

            SharedAccountNoteResponse response = sharedAccountNoteService.createNote(USER_ID, requestDto);

            assertEquals(USER_ID, response.userId());
            assertEquals(USERNAME, response.username());
            verify(sharedAccountNoteRepository, times(1)).save(any(SharedAccountNote.class));
        }

        @Test
        void shouldSaveNoteWithOwnerAndMemberIdFromParticipantsResponse() {
            SharedAccountNoteDto requestDto = buildNoteDto();
            SharedAccountParticipantsResponse participantsResponse =
                    new SharedAccountParticipantsResponse(OWNER_ID, MEMBER_ID);

            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
            when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);

            ArgumentCaptor<SharedAccountNote> noteCaptor = ArgumentCaptor.forClass(SharedAccountNote.class);

            sharedAccountNoteService.createNote(USER_ID, requestDto);

            verify(sharedAccountNoteRepository).save(noteCaptor.capture());
            SharedAccountNote savedNote = noteCaptor.getValue();
            assertEquals(OWNER_ID, savedNote.getOwnerId());
            assertEquals(MEMBER_ID, savedNote.getMemberId());
            assertEquals(USER_ID, savedNote.getCreatedByUserId());
            assertEquals(TOPIC, savedNote.getTopic());
            assertEquals(DESCRIPTION, savedNote.getDescription());
        }

        @Test
        void shouldSetCreatedAtToCurrentTimeWhenCreatingNote() {
            SharedAccountNoteDto requestDto = buildNoteDto();
            SharedAccountParticipantsResponse participantsResponse =
                    new SharedAccountParticipantsResponse(OWNER_ID, MEMBER_ID);

            when(sharedAccountParticipantsService.getParticipants(USER_ID)).thenReturn(participantsResponse);
            when(authBackendClient.getUsername(USER_ID)).thenReturn(USERNAME);

            LocalDateTime beforeCall = LocalDateTime.now();
            ArgumentCaptor<SharedAccountNote> noteCaptor = ArgumentCaptor.forClass(SharedAccountNote.class);

            sharedAccountNoteService.createNote(USER_ID, requestDto);

            verify(sharedAccountNoteRepository).save(noteCaptor.capture());
            LocalDateTime afterCall = LocalDateTime.now();
            LocalDateTime savedCreatedAt = noteCaptor.getValue().getCreatedAt();

            assertThat(savedCreatedAt).isBetween(beforeCall, afterCall);
        }

        @Test
        void shouldNotSaveNoteWhenParticipantsServiceThrowsException() {
            SharedAccountNoteDto requestDto = buildNoteDto();

            when(sharedAccountParticipantsService.getParticipants(USER_ID))
                    .thenThrow(new RequestedEntityNotFoundException("Shared account not found"));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> sharedAccountNoteService.createNote(USER_ID, requestDto));

            verifyNoInteractions(sharedAccountNoteRepository);
        }
    }

    @Nested
    class EditNote {

        @Test
        void shouldEditNoteWhenUserIsOwner() {
            SharedAccountNote existingNote = buildNote(NOTE_ID, OWNER_ID, MEMBER_ID, MEMBER_ID);
            SharedAccountNoteDto requestDto = new SharedAccountNoteDto(null, "New Topic", "New description", null, null, null);

            when(sharedAccountNoteRepository.findById(NOTE_ID)).thenReturn(Optional.of(existingNote));

            Long result = sharedAccountNoteService.editNote(OWNER_ID, NOTE_ID, requestDto);

            assertEquals(NOTE_ID, result);
            assertEquals("New Topic", existingNote.getTopic());
            assertEquals("New description", existingNote.getDescription());
            verify(sharedAccountNoteRepository, times(1)).save(existingNote);
        }

        @Test
        void shouldEditNoteWhenUserIsMember() {
            SharedAccountNote existingNote = buildNote(NOTE_ID, OWNER_ID, MEMBER_ID, OWNER_ID);
            SharedAccountNoteDto requestDto = new SharedAccountNoteDto(null, "New Topic", "New description", null, null, null);

            when(sharedAccountNoteRepository.findById(NOTE_ID)).thenReturn(Optional.of(existingNote));

            Long result = sharedAccountNoteService.editNote(MEMBER_ID, NOTE_ID, requestDto);

            assertEquals(NOTE_ID, result);
            verify(sharedAccountNoteRepository, times(1)).save(existingNote);
        }

        @Test
        void shouldThrowExceptionWhenNoteDoesNotExist() {
            SharedAccountNoteDto requestDto = buildNoteDto();

            when(sharedAccountNoteRepository.findById(NOTE_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> sharedAccountNoteService.editNote(USER_ID, NOTE_ID, requestDto));
        }

        @Test
        void shouldThrowExceptionWhenUserIsNeitherOwnerNorMember() {
            SharedAccountNote existingNote = buildNote(NOTE_ID, OWNER_ID, MEMBER_ID, OWNER_ID);
            SharedAccountNoteDto requestDto = buildNoteDto();
            Long strangerId = 999L;

            when(sharedAccountNoteRepository.findById(NOTE_ID)).thenReturn(Optional.of(existingNote));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> sharedAccountNoteService.editNote(strangerId, NOTE_ID, requestDto));
        }

        @Test
        void shouldNotSaveNoteWhenUserIsNeitherOwnerNorMember() {
            SharedAccountNote existingNote = buildNote(NOTE_ID, OWNER_ID, MEMBER_ID, OWNER_ID);
            SharedAccountNoteDto requestDto = buildNoteDto();
            Long strangerId = 999L;

            when(sharedAccountNoteRepository.findById(NOTE_ID)).thenReturn(Optional.of(existingNote));

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> sharedAccountNoteService.editNote(strangerId, NOTE_ID, requestDto));

            verify(sharedAccountNoteRepository, never()).save(any(SharedAccountNote.class));
        }
    }

    @Nested
    class GetNotes {

        @Test
        void shouldReturnListOfNotesWhenNotesExist() {
            SharedAccountNote note1 = buildNote(1L, OWNER_ID, MEMBER_ID, OWNER_ID);
            SharedAccountNote note2 = buildNote(2L, OWNER_ID, MEMBER_ID, MEMBER_ID);

            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(USER_ID))
                    .thenReturn(List.of(note1, note2));
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn("owner_user");
            when(authBackendClient.getUsername(MEMBER_ID)).thenReturn("member_user");

            List<SharedAccountNoteDto> result = sharedAccountNoteService.getNotes(USER_ID);

            assertEquals(2, result.size());
            assertEquals(TOPIC, result.getFirst().topic());
            assertEquals(DESCRIPTION, result.getFirst().description());
        }

        @Test
        void shouldMapNoteCreatorUsernameCorrectlyForEachNote() {
            SharedAccountNote note1 = buildNote(1L, OWNER_ID, MEMBER_ID, OWNER_ID);

            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(USER_ID))
                    .thenReturn(List.of(note1));
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn("owner_user");

            List<SharedAccountNoteDto> result = sharedAccountNoteService.getNotes(USER_ID);

            assertEquals("owner_user", result.getFirst().noteCreatorUsername());
            assertEquals(OWNER_ID, result.getFirst().noteCreatorId());
        }

        @Test
        void shouldCallGetUsernameOnlyOnceForDuplicateCreators() {
            SharedAccountNote note1 = buildNote(1L, OWNER_ID, MEMBER_ID, OWNER_ID);
            SharedAccountNote note2 = buildNote(2L, OWNER_ID, MEMBER_ID, OWNER_ID);

            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(USER_ID))
                    .thenReturn(List.of(note1, note2));
            when(authBackendClient.getUsername(OWNER_ID)).thenReturn("owner_user");

            sharedAccountNoteService.getNotes(USER_ID);

            verify(authBackendClient, times(1)).getUsername(OWNER_ID);
        }

        @Test
        void shouldReturnEmptyListWhenNoNotesExistForUser() {
            when(sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(USER_ID)).thenReturn(List.of());

            List<SharedAccountNoteDto> result = sharedAccountNoteService.getNotes(USER_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(authBackendClient);
        }
    }

    @Nested
    class DeleteNote {

        @Test
        void shouldDeleteNoteWhenNoteExistsAndUserIsOwnerOrMember() {
            SharedAccountNote existingNote = buildNote(NOTE_ID, OWNER_ID, MEMBER_ID, OWNER_ID);

            when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, USER_ID))
                    .thenReturn(Optional.of(existingNote));

            sharedAccountNoteService.deleteNote(USER_ID, NOTE_ID);

            verify(sharedAccountNoteRepository, times(1)).delete(existingNote);
        }

        @Test
        void shouldThrowExceptionWhenNoteNotFoundForDeletion() {
            when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> sharedAccountNoteService.deleteNote(USER_ID, NOTE_ID));
        }

        @Test
        void shouldNotCallDeleteWhenNoteNotFound() {
            when(sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(NOTE_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class,
                    () -> sharedAccountNoteService.deleteNote(USER_ID, NOTE_ID));

            verify(sharedAccountNoteRepository, never()).delete(any(SharedAccountNote.class));
        }
    }
}