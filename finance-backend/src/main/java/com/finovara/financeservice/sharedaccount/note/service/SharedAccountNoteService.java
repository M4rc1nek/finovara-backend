package com.finovara.financeservice.sharedaccount.note.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.sharedaccount.note.dto.SharedAccountNoteDto;
import com.finovara.financeservice.sharedaccount.note.dto.SharedAccountNoteResponse;
import com.finovara.financeservice.sharedaccount.note.model.SharedAccountNote;
import com.finovara.financeservice.sharedaccount.note.repository.SharedAccountNoteRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SharedAccountNoteService {

    private final SharedAccountNoteRepository sharedAccountNoteRepository;
    private final SharedAccountParticipantsService sharedAccountParticipantsService;
    private final AuthBackendClient authBackendClient;

    @Transactional
    public SharedAccountNoteResponse createNote(Long userId, SharedAccountNoteDto sharedAccountNoteDto) {

        SharedAccountParticipantsResponse sharedAccountParticipantsResponse = sharedAccountParticipantsService.getParticipants(userId);
        String createdByUsername = authBackendClient.getUsername(userId);

        SharedAccountNote sharedAccountNote = SharedAccountNote.builder()
                .topic(sharedAccountNoteDto.topic())
                .description(sharedAccountNoteDto.description())
                .createdAt(LocalDateTime.now())
                .ownerId(sharedAccountParticipantsResponse.ownerId())
                .memberId(sharedAccountParticipantsResponse.memberId())
                .createdByUserId(userId)
                .build();

        sharedAccountNoteRepository.save(sharedAccountNote);

        return new SharedAccountNoteResponse(sharedAccountNote.getId(), userId, createdByUsername);
    }

    @Transactional
    public Long editNote(Long userId, Long noteId, SharedAccountNoteDto sharedAccountNoteDto) {
        SharedAccountNote existingNote = getNoteByIdOrThrow(noteId);

        if (!isOwnerOrMember(existingNote, userId)) {
            throw new RequestedEntityNotFoundException("Note not found for this user");
        }

        existingNote.setTopic(sharedAccountNoteDto.topic());
        existingNote.setDescription(sharedAccountNoteDto.description());

        sharedAccountNoteRepository.save(existingNote);

        return noteId;
    }

    public List<SharedAccountNoteDto> getNotes(Long userId) {
        List<SharedAccountNote> notes = sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(userId);

        Map<Long, String> usernameById = notes.stream()
                .map(SharedAccountNote::getCreatedByUserId)
                .distinct()
                .collect(Collectors.toMap(id -> id, authBackendClient::getUsername));

        return notes.stream()
                .map(note -> new SharedAccountNoteDto(
                        note.getId(),
                        note.getTopic(),
                        note.getDescription(),
                        note.getCreatedAt(),
                        note.getCreatedByUserId(),
                        usernameById.get(note.getCreatedByUserId())
                ))
                .toList();
    }

    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        SharedAccountNote note = sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(noteId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Note not found"));

        sharedAccountNoteRepository.delete(note);
    }

    private SharedAccountNote getNoteByIdOrThrow(Long noteId) {
        return sharedAccountNoteRepository.findById(noteId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Note not found"));
    }

    private boolean isOwnerOrMember(SharedAccountNote note, Long userId) {
        return note.getOwnerId().equals(userId) || note.getMemberId().equals(userId);
    }
}