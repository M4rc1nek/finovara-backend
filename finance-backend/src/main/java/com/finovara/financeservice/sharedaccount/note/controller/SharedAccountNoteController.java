package com.finovara.financeservice.sharedaccount.note.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.sharedaccount.note.dto.SharedAccountNoteDto;
import com.finovara.financeservice.sharedaccount.note.dto.SharedAccountNoteResponse;
import com.finovara.financeservice.sharedaccount.note.service.SharedAccountNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shared-accounts/note")
@RequiredArgsConstructor
public class SharedAccountNoteController {

    private final SharedAccountNoteService sharedAccountNoteService;

    @PostMapping
    public ResponseEntity<SharedAccountNoteResponse> addSharedNote(@RequestBody @Valid SharedAccountNoteDto sharedAccountNoteDto) {
        return ResponseEntity.ok(sharedAccountNoteService.createNote(SecurityUtils.getCurrentUserId(), sharedAccountNoteDto));
    }

    @PutMapping("/edit/{noteId}")
    public ResponseEntity<Long> editSharedNote(@RequestBody @Valid SharedAccountNoteDto sharedAccountNoteDto, @PathVariable Long noteId) {
        return ResponseEntity.ok(sharedAccountNoteService.editNote(SecurityUtils.getCurrentUserId(), noteId, sharedAccountNoteDto));
    }

    @GetMapping
    public ResponseEntity<List<SharedAccountNoteDto>> getSharedNote() {
        return ResponseEntity.ok(sharedAccountNoteService.getNotes(SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteSharedNote(@PathVariable Long noteId) {
        sharedAccountNoteService.deleteNote(SecurityUtils.getCurrentUserId(), noteId);
        return ResponseEntity.noContent().build();
    }

}
