package com.finovara.financeservice.sharedaccount.note.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.financeservice.sharedaccount.note.dto.SharedAccountNoteDto;
import com.finovara.financeservice.sharedaccount.note.dto.SharedAccountNoteResponse;
import com.finovara.financeservice.sharedaccount.note.model.SharedAccountNote;
import com.finovara.financeservice.sharedaccount.note.repository.SharedAccountNoteRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.util.sharedaccount.SharedUsernameResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
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
    private final SharedUsernameResolver usernameResolver;
    private final OutboxService outboxService;

    @Value("${finance.pagination.page-size}")
    private int pageSize;

    @Transactional
    public SharedAccountNoteResponse createNote(Long userId, SharedAccountNoteDto dto) {
        SharedAccountParticipantsResponse participants = sharedAccountParticipantsService.getParticipants(userId);
        String createdByUsername = usernameResolver.resolve(List.of(userId)).get(userId);

        SharedAccountNote saved = sharedAccountNoteRepository.save(SharedAccountNote.builder()
                .topic(dto.topic())
                .description(dto.description())
                .createdAt(LocalDateTime.now())
                .ownerId(participants.ownerId())
                .memberId(participants.memberId())
                .createdByUserId(userId)
                .build());

        logActivity(participants, userId, saved.getId(), SharedAccountActivityLogType.NOTE_CREATED);

        return new SharedAccountNoteResponse(saved.getId(), userId, createdByUsername);
    }

    @Transactional
    public Long editNote(Long userId, Long noteId, SharedAccountNoteDto dto) {
        SharedAccountNote existing = getNoteByIdOrThrow(userId, noteId);

        existing.setTopic(dto.topic());
        existing.setDescription(dto.description());

        logActivity(sharedAccountParticipantsService.getParticipants(userId), userId, noteId, SharedAccountActivityLogType.NOTE_EDITED);

        return noteId;
    }

    public List<SharedAccountNoteDto> getNotes(Long userId) {
        List<SharedAccountNote> notes = sharedAccountNoteRepository.findAllByOwnerIdOrMemberId(userId, PageRequest.of(0, pageSize));

        Map<Long, String> usernameById = usernameResolver.resolve(
                notes.stream().map(SharedAccountNote::getCreatedByUserId).collect(Collectors.toSet()));

        return notes.stream()
                .map(note -> new SharedAccountNoteDto(
                        note.getId(),
                        note.getTopic(),
                        note.getDescription(),
                        note.getCreatedAt(),
                        note.getCreatedByUserId(),
                        usernameById.get(note.getCreatedByUserId())))
                .toList();
    }

    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        SharedAccountNote note = getNoteByIdOrThrow(userId, noteId);

        sharedAccountNoteRepository.delete(note);
        logActivity(sharedAccountParticipantsService.getParticipants(userId), userId, noteId, SharedAccountActivityLogType.NOTE_DELETED);
    }

    private void logActivity(SharedAccountParticipantsResponse participants, Long userId, Long noteId, SharedAccountActivityLogType type) {
        outboxService.save("SharedAccountNote", noteId.toString(), "shared-account.activity",
                new SharedAccountActivityLogEvent(participants.ownerId(), participants.memberId(), userId, noteId, type, LocalDateTime.now()));
    }

    private SharedAccountNote getNoteByIdOrThrow(Long userId, Long noteId) {
        return sharedAccountNoteRepository.findByIdAndOwnerIdOrMemberId(noteId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Note not found"));
    }
}