package com.xtended.subscriptionservice.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtended.subscriptionservice.subscription.model.Outbox;
import com.xtended.subscriptionservice.subscription.repository.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для OutboxPersistenceService
 */
@ExtendWith(MockitoExtension.class)
class OutboxPersistenceServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxPersistenceService outboxPersistenceService;

    private static final String ROUTING_KEY = "test.routing.key";
    private static final String AGGREGATE_ID = "aggregate-123";
    private static final String JSON_PAYLOAD = "{\"data\":\"test data\"}";
    private static final Object TEST_PAYLOAD = new TestData("test data");

    @Test
    @DisplayName("Должен успешно сохранить outbox событие с корректными параметрами")
    void saveOutboxEvent_SuccessfulSave() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(TEST_PAYLOAD)).thenReturn(JSON_PAYLOAD);
        when(outboxEventRepository.save(any(Outbox.class))).thenReturn(createTestOutbox());

        assertDoesNotThrow(() -> outboxPersistenceService.saveOutboxEvent(ROUTING_KEY, TEST_PAYLOAD, AGGREGATE_ID));

        verify(objectMapper).writeValueAsString(TEST_PAYLOAD);
        verify(outboxEventRepository).save(argThat(outbox ->
                outbox.getAggregateId().equals(AGGREGATE_ID) &&
                        outbox.getRoutingKey().equals(ROUTING_KEY) &&
                        outbox.getEventType().equals(ROUTING_KEY) &&
                        outbox.getPayload().equals(JSON_PAYLOAD)
        ));
    }

    @Test
    @DisplayName("Должен выбросить RuntimeException при ошибке сериализации JSON")
    void saveOutboxEvent_JsonProcessingException_ThrowsRuntimeException() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(TEST_PAYLOAD))
                .thenThrow(new JsonProcessingException("JSON serialization error") {
                });

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                outboxPersistenceService.saveOutboxEvent(ROUTING_KEY, TEST_PAYLOAD, AGGREGATE_ID)
        );

        assertInstanceOf(JsonProcessingException.class, exception.getCause());
        assertEquals("JSON serialization error", exception.getCause().getMessage());
        verify(objectMapper).writeValueAsString(TEST_PAYLOAD);
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    @DisplayName("Должен корректно обработать null payload")
    void saveOutboxEvent_NullPayload_HandlesGracefully() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(null)).thenReturn("null");
        when(outboxEventRepository.save(any(Outbox.class))).thenReturn(createTestOutbox());

        assertDoesNotThrow(() -> outboxPersistenceService.saveOutboxEvent(ROUTING_KEY, null, AGGREGATE_ID));

        verify(objectMapper).writeValueAsString(null);
        verify(outboxEventRepository).save(argThat(outbox ->
                outbox.getAggregateId().equals(AGGREGATE_ID) &&
                        outbox.getRoutingKey().equals(ROUTING_KEY) &&
                        outbox.getEventType().equals(ROUTING_KEY) &&
                        outbox.getPayload().equals("null")
        ));
    }

    @Test
    @DisplayName("Должен корректно обработать пустые параметры routingKey и aggregateId")
    void saveOutboxEvent_EmptyParameters_HandlesGracefully() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(TEST_PAYLOAD)).thenReturn(JSON_PAYLOAD);
        when(outboxEventRepository.save(any(Outbox.class))).thenReturn(createTestOutbox());

        assertDoesNotThrow(() -> outboxPersistenceService.saveOutboxEvent("", TEST_PAYLOAD, ""));

        verify(objectMapper).writeValueAsString(TEST_PAYLOAD);
        verify(outboxEventRepository).save(argThat(outbox ->
                outbox.getAggregateId().isEmpty() &&
                        outbox.getRoutingKey().isEmpty() &&
                        outbox.getEventType().isEmpty() &&
                        outbox.getPayload().equals(JSON_PAYLOAD)
        ));
    }

    @Test
    @DisplayName("Должен распространить RuntimeException при ошибке сохранения в репозитории")
    void saveOutboxEvent_RepositorySaveException_PropagatesAsRuntimeException() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(TEST_PAYLOAD)).thenReturn(JSON_PAYLOAD);
        when(outboxEventRepository.save(any(Outbox.class)))
                .thenThrow(new RuntimeException("Database error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                outboxPersistenceService.saveOutboxEvent(ROUTING_KEY, TEST_PAYLOAD, AGGREGATE_ID)
        );

        assertEquals("Database error", exception.getMessage());
        verify(objectMapper).writeValueAsString(TEST_PAYLOAD);
        verify(outboxEventRepository).save(any(Outbox.class));
    }

    @Test
    @DisplayName("Должен корректно сериализовать сложный объект payload")
    void saveOutboxEvent_ComplexPayload_SerializesCorrectly() throws JsonProcessingException {
        ComplexTestData complexPayload = new ComplexTestData("test", 123, true);
        String expectedJson = "{\"name\":\"test\",\"number\":123,\"active\":true}";

        when(objectMapper.writeValueAsString(complexPayload)).thenReturn(expectedJson);
        when(outboxEventRepository.save(any(Outbox.class))).thenReturn(createTestOutbox());

        assertDoesNotThrow(() -> outboxPersistenceService.saveOutboxEvent(ROUTING_KEY, complexPayload, AGGREGATE_ID));

        verify(objectMapper).writeValueAsString(complexPayload);
        verify(outboxEventRepository).save(argThat(outbox ->
                outbox.getAggregateId().equals(AGGREGATE_ID) &&
                        outbox.getRoutingKey().equals(ROUTING_KEY) &&
                        outbox.getEventType().equals(ROUTING_KEY) &&
                        outbox.getPayload().equals(expectedJson)
        ));
    }

    private Outbox createTestOutbox() {
        return Outbox.builder()
                .id(UUID.randomUUID())
                .aggregateId(AGGREGATE_ID)
                .eventType(ROUTING_KEY)
                .routingKey(ROUTING_KEY)
                .payload(JSON_PAYLOAD)
                .build();
    }

    private record TestData(String data) {
    }

    private record ComplexTestData(String name, int number, boolean active) {
    }
}
