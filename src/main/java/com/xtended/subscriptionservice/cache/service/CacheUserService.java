package com.xtended.subscriptionservice.cache.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtended.subscriptionservice.cache.model.UserCacheInfo;
import com.xtended.subscriptionservice.subscription.dto.InvoiceEvent;
import com.xtended.subscriptionservice.subscription.dto.SubscriptionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Сервис для управления кэшем пользователей
 */
@Component
@RequiredArgsConstructor
public class CacheUserService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String USER_SUBSCRIPTION_KEY = "user:%s:subscription";
    private static final String USER_INVOICES_KEY = "user:%s:invoices";

    /**
     * Добавляет активную подписку в кэш
     *
     * @param userId ID пользователя
     * @param event  событие подписки
     */
    public void addActiveSubscription(UUID userId, SubscriptionEvent event) {
        String key = String.format(USER_SUBSCRIPTION_KEY, userId);
        redisTemplate.opsForValue().set(key, event);

    }

    /**
     * Удаляет подписку из кеша
     *
     * @param userId            ID пользователя
     */
    public void removeSubscription(UUID userId) {
        String key = String.format(USER_SUBSCRIPTION_KEY, userId);
        redisTemplate.delete(key);
    }

    /**
     * Добавляет выставленный счет в кэш
     *
     * @param userId ID пользователя
     * @param event  событие счета
     */
    public void addInvoice(UUID userId, InvoiceEvent event) {
        String key = String.format(USER_INVOICES_KEY, userId);
        try {
            String json = objectMapper.writeValueAsString(event);
            // Score = epoch day или миллисекунды
            redisTemplate.opsForZSet().add(key, json, event.getInvoiceDate().toEpochDay());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Получение данных о пользователе из кэша
     *
     * @param userId   ID пользователя
     * @param pageable параметры пагинации
     * @return информация о пользователе
     */
    public UserCacheInfo getUserCacheInfo(UUID userId, Pageable pageable) {
        String subsKey = String.format(USER_SUBSCRIPTION_KEY, userId);
        SubscriptionEvent activeSubscription = (SubscriptionEvent) redisTemplate.opsForValue().get(subsKey);

        String invKey = String.format(USER_INVOICES_KEY, userId);
        Long total = redisTemplate.opsForZSet().zCard(invKey);
        int start = (int) pageable.getOffset();
        int end = start + pageable.getPageSize() - 1;
        // Получаем в обратном хронологическом порядке
        Set<Object> invPage = redisTemplate.opsForZSet().reverseRange(invKey, start, end);
        List<InvoiceEvent> invoices = invPage.stream()
                .map(obj -> {
                    try {
                        return objectMapper.readValue((String) obj, InvoiceEvent.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();

        return new UserCacheInfo(userId, activeSubscription,
                new PageImpl<>(invoices, pageable, total == null ? 0 : total));
    }
}
