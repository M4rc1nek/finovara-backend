package com.finovara.corebackend.contact.controller;

import com.finovara.corebackend.contact.dto.ContactDto;
import com.finovara.corebackend.contact.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/api/contact")
    public ResponseEntity<Void> sendMessage(@Valid @RequestBody ContactDto contactDto) {
        contactService.requestContactEmail(contactDto);
        return ResponseEntity.accepted().build();
    }
}