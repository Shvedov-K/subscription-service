package com.xtended.subscriptionservice.cache.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtended.subscriptionservice.cache.service.CacheUserService;
import com.xtended.subscriptionservice.subscription.dto.InvoiceEvent;
import com.xtended.subscriptionservice.subscription.dto.SubscriptionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Класс-слушатель для обработки событий из очереди
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheUpdateListener {

    private final CacheUserService cacheUserService;
    private final ObjectMapper objectMapper;

    /**
     * Обработчик события активации подписки
     *
     * @param message JSON-строка с событием
     */
    @RabbitListener(queues = "subscription.activated.queue")
    public void handleSubscriptionActivated(String message) {
        try {
            SubscriptionEvent event = objectMapper.readValue(message, SubscriptionEvent.class);
            cacheUserService.addActiveSubscription(event.getUserId(), event);
        } catch (Exception e) {
            log.error("Error processing subscription.activated event", e);
        }
    }

    /**
     * Обработчик события деактивации подписки
     *
     * @param message JSON-строка с событием
     */
    @RabbitListener(queues = "subscription.deactivated.queue")
    public void handleSubscriptionDeactivated(String message) {
        try {
            SubscriptionEvent event = objectMapper.readValue(message, SubscriptionEvent.class);
            cacheUserService.removeSubscription(event.getUserId());
        } catch (Exception e) {
            log.error("Error processing subscription.deactivated event", e);
        }
    }

    /**
     * Обработчик события выставленного счета
     *
     * @param message JSON-строка с событием
     */
    @RabbitListener(queues = "invoice.issued.queue")
    public void handleInvoiceIssued(String message) {
        try {
            InvoiceEvent event = objectMapper.readValue(message, InvoiceEvent.class);
            cacheUserService.addInvoice(event.getUserId(), event);
        } catch (Exception e) {
            log.error("Error processing invoice issued event", e);
        }
    }
}
