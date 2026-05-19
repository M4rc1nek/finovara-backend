package com.finovara.finovarabackend.contact.service;

import com.finovara.finovarabackend.contact.dto.ContactDto;
import com.finovara.finovarabackend.util.email.EmailDomainValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final EmailDomainValidator emailDomainValidator;
    private final ContactSendEmail contactSendEmail;

    public void requestContactEmail(ContactDto dto) {
        log.info("Requested contact email started.");
        emailDomainValidator.validateDomainHasMxRecord(dto.email());
        contactSendEmail.sendContactEmail(dto);
    }

}
