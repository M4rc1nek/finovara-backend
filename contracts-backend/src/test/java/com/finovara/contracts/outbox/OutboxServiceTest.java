package com.finovara.contracts.outbox;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxService outboxService;

    @Nested
    class SaveTest {

        @Test
        void shouldSerializePayloadAndPersistEvent() throws Exception {
            TestPayload payload = new TestPayload("hello");
            when(objectMapper.writeValueAsString(payload)).thenReturn("{\"value\":\"hello\"}");

            outboxService.save("PiggyBank", "123", "activity.piggybank", payload);

            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(captor.capture());

            OutboxEvent saved = captor.getValue();
            assertThat(saved.getAggregateType()).isEqualTo("PiggyBank");
            assertThat(saved.getAggregateId()).isEqualTo("123");
            assertThat(saved.getEventType()).isEqualTo("activity.piggybank");
            assertThat(saved.getPayload()).isEqualTo("{\"value\":\"hello\"}");
            assertThat(saved.getPayloadType()).isEqualTo(TestPayload.class.getName());
            assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenSerializationFails() throws Exception {
            TestPayload payload = new TestPayload("hello");
            when(objectMapper.writeValueAsString(payload)).thenThrow(new RuntimeException("json error"));

            assertThatThrownBy(() -> outboxService.save("PiggyBank", "123", "activity.piggybank", payload))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot serialize event")
                    .hasMessageContaining("activity.piggybank");
        }

        @Test
        void shouldNotCallRepositoryWhenSerializationFails() throws Exception {
            when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException());

            assertThatThrownBy(() -> outboxService.save("PiggyBank", "123", "activity.piggybank", new TestPayload("x")))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(outboxEventRepository);
        }
    }

    record TestPayload(String value) {}
}