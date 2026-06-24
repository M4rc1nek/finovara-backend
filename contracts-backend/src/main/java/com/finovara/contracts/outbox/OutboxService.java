package com.finovara.contracts.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void save(String aggregateType, String aggregateId, String eventType, Object eventPayload) {
        try {
            String payload = objectMapper.writeValueAsString(eventPayload);
            OutboxEvent event = OutboxEvent.of(aggregateType, aggregateId, eventType, payload, eventPayload.getClass().getName());
            outboxEventRepository.save(event);
        } catch (Exception e) {
            throw new IllegalArgumentException("Nie można zserializować eventu: " + eventType, e);
        }
    }
}