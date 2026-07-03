package com.finovara.contracts.outbox;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    @SuppressWarnings("unchecked")
    private void stubSuccessfulSend() {
        SendResult<String, Object> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
    }

    @Nested
    class PublishPendingEventsTest {
        @Test
        void shouldMarkEventAsSentWhenKafkaSendSucceeds() throws Exception {
            OutboxEvent event = pendingEvent("activity.piggybank", "123");
            when(outboxEventRepository.findPendingEvents(OutboxStatus.PENDING)).thenReturn(List.of(event));
            when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(new Object());
            stubSuccessfulSend();

            outboxEventPublisher.publishPendingEvents();

            assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
            assertThat(event.getSentAt()).isNotNull();
            verify(kafkaTemplate).send(any(Message.class));
        }

        @Test
        void shouldMarkEventAsFailedWhenKafkaThrows() throws Exception {
            OutboxEvent event = pendingEvent("activity.piggybank", "123");
            when(outboxEventRepository.findPendingEvents(OutboxStatus.PENDING)).thenReturn(List.of(event));
            when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(new Object());
            when(kafkaTemplate.send(any(Message.class))).thenThrow(new RuntimeException("Kafka down"));

            outboxEventPublisher.publishPendingEvents();

            assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
            assertThat(event.getSentAt()).isNull();
        }

        @Test
        void shouldMarkEventAsFailedWhenKafkaFutureCompletesExceptionally() throws Exception {
            OutboxEvent event = pendingEvent("activity.piggybank", "123");
            when(outboxEventRepository.findPendingEvents(OutboxStatus.PENDING)).thenReturn(List.of(event));
            when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(new Object());

            CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("broker unreachable"));
            when(kafkaTemplate.send(any(Message.class))).thenReturn(failedFuture);

            outboxEventPublisher.publishPendingEvents();

            assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
            assertThat(event.getSentAt()).isNull();
        }

        @Test
        void shouldMarkEventAsFailedWhenDeserializationFails() throws Exception {
            OutboxEvent event = pendingEvent("activity.piggybank", "123");
            when(outboxEventRepository.findPendingEvents(OutboxStatus.PENDING)).thenReturn(List.of(event));
            when(objectMapper.readValue(anyString(), any(Class.class))).thenThrow(new RuntimeException("bad json"));

            outboxEventPublisher.publishPendingEvents();

            assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldProcessAllEventsEvenIfOneFails() throws Exception {
            OutboxEvent failingEvent = pendingEvent("activity.piggybank", "111");
            OutboxEvent successEvent = pendingEvent("activity.piggybank", "222");

            when(outboxEventRepository.findPendingEvents(OutboxStatus.PENDING)).thenReturn(List.of(failingEvent, successEvent));
            when(objectMapper.readValue(anyString(), any(Class.class)))
                    .thenThrow(new RuntimeException("bad json"))
                    .thenReturn(new Object());
            stubSuccessfulSend();

            outboxEventPublisher.publishPendingEvents();

            assertThat(failingEvent.getStatus()).isEqualTo(OutboxStatus.FAILED);
            assertThat(successEvent.getStatus()).isEqualTo(OutboxStatus.SENT);
        }

        @Test
        void shouldDoNothingWhenNoPendingEvents() {
            when(outboxEventRepository.findPendingEvents(OutboxStatus.PENDING)).thenReturn(List.of());

            outboxEventPublisher.publishPendingEvents();

            verifyNoInteractions(kafkaTemplate);
            verifyNoInteractions(objectMapper);
        }

        @Test
        void shouldSendToCorrectTopicAndWithCorrectKey() throws Exception {
            OutboxEvent event = pendingEvent("activity.piggybank", "abc-123");
            when(outboxEventRepository.findPendingEvents(OutboxStatus.PENDING)).thenReturn(List.of(event));
            when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(new Object());
            stubSuccessfulSend();

            ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);

            outboxEventPublisher.publishPendingEvents();

            verify(kafkaTemplate).send(messageCaptor.capture());
            Message<?> sent = messageCaptor.getValue();
            assertThat(sent.getHeaders().get("kafka_topic")).isEqualTo("activity.piggybank");
            assertThat(sent.getHeaders().get("kafka_messageKey")).isEqualTo("abc-123");
        }
    }

    @Nested
    class CleanOldEventsTest {

        @Test
        void shouldDeleteSentEventsOlderThan7Days() {
            outboxEventPublisher.cleanOldEvents();

            ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(outboxEventRepository).deleteByStatusAndSentAtBefore(eq(OutboxStatus.SENT), dateCaptor.capture());

            LocalDateTime captured = dateCaptor.getValue();
            assertThat(captured).isBetween(LocalDateTime.now().minusDays(7).minusSeconds(5), LocalDateTime.now().minusDays(7).plusSeconds(5));
        }

        @Test
        void shouldDeleteOnlySentEvents() {
            outboxEventPublisher.cleanOldEvents();

            verify(outboxEventRepository).deleteByStatusAndSentAtBefore(eq(OutboxStatus.SENT), any());
        }
    }

    private OutboxEvent pendingEvent(String eventType, String aggregateId) {
        return OutboxEvent.of("PiggyBank", aggregateId, eventType, "{\"value\":\"test\"}", Object.class.getName());
    }
}