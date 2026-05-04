package com.xtended.subscriptionservice.subscription.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtended.subscriptionservice.subscription.service.OutboxPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


/**
 * Обработчик событий из outbox
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxFallbackEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxPersistenceService outboxPersistenceService;

    /**
     * Запланировать отправку после коммита. Если брокер упал – сохранить в outbox.
     */
    public void publishAfterCommit(String routingKey, Object payload, String aggregateId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    String json = objectMapper.writeValueAsString(payload);   // сериализуем в JSON
                    rabbitTemplate.convertAndSend(routingKey, json);
                } catch (Exception ex) {
                    log.warn("Failed to send event to RabbitMQ, saving to outbox", ex);
                    outboxPersistenceService.saveOutboxEvent(routingKey, payload, aggregateId);
                }
            }
        });
    }
}
