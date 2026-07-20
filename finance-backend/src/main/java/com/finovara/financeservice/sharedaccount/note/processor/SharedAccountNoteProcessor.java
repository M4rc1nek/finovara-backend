package com.finovara.financeservice.sharedaccount.note.processor;

import com.finovara.financeservice.sharedaccount.note.repository.SharedAccountNoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SharedAccountNoteProcessor {

    private final SharedAccountNoteRepository sharedAccountNoteRepository;

    public void deleteNotes(){
        sharedAccountNoteRepository.deleteAllInBatch();
        log.info("Deleted shared-account notes");
    }

}
