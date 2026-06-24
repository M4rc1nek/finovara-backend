package com.finovara.contracts.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.scheduler.delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findPendingEvents(OutboxStatus.PENDING);

        for (OutboxEvent event : events) {
            try {
                Class<?> eventClass = Class.forName(event.getPayloadType());
                Object deserializedPayload = objectMapper.readValue(event.getPayload(), eventClass);

                kafkaTemplate.send(MessageBuilder
                                .withPayload(deserializedPayload)
                                .setHeader(KafkaHeaders.TOPIC, event.getEventType())
                                .setHeader(KafkaHeaders.KEY, event.getAggregateId())
                                .build());

                event.markSent();
                log.debug("Outbox: event {} sent for {}/{}", event.getEventType(), event.getAggregateType(), event.getAggregateId());

            } catch (Exception e) {
                event.markFailed();
                log.error("Outbox: failed to send event {} for {}/{}", event.getEventType(), event.getAggregateType(), event.getAggregateId(), e);
            }
        }
    }

    @Scheduled(cron = "${scheduler.clean.old-events:0 0 3 * * *}")
    @Transactional
    public void cleanOldEvents() {
        outboxEventRepository.deleteByStatusAndSentAtBefore(OutboxStatus.SENT, LocalDateTime.now().minusDays(7));
    }
}