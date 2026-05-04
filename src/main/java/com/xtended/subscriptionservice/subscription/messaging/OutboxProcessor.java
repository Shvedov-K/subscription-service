package com.xtended.subscriptionservice.subscription.messaging;

import com.xtended.subscriptionservice.subscription.model.Outbox;
import com.xtended.subscriptionservice.subscription.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Обработчик outbox
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {
    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Шедулированный метод для обработки событий из outbox.
     * Проверяет наличие записей в outbox и пытается отправить их в RabbitMQ.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processPendingEvents() {
        List<Outbox> events = repository.findAll();
        for (Outbox event : events) {
            try {
                rabbitTemplate.convertAndSend(event.getRoutingKey(), event.getPayload());
                repository.delete(event);
            } catch (Exception e) {
                log.error("Outbox event {} still cannot be sent", event.getId(), e);
            }
        }
    }
}
