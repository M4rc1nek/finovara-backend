package com.finovara.financeservice.sharedaccount.note.dto;

public record SharedAccountNoteResponse(
        Long noteId,
        Long userId,
        String username
){
}
