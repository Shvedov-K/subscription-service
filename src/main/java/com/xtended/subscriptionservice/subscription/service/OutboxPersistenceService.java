package com.xtended.subscriptionservice.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtended.subscriptionservice.subscription.model.Outbox;
import com.xtended.subscriptionservice.subscription.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для работы с outbox
 */
@Service
@RequiredArgsConstructor
public class OutboxPersistenceService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Сохранение события в outbox
     *
     * @param routingKey  ключ маршрутизации события
     * @param payload     полезная нагрузка события
     * @param aggregateId идентификатор
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOutboxEvent(String routingKey, Object payload, String aggregateId) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            Outbox event = Outbox.builder()
                    .aggregateId(aggregateId)
                    .eventType(routingKey)
                    .routingKey(routingKey)
                    .payload(json)
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
