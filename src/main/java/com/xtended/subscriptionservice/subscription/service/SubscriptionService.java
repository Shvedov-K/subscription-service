package com.xtended.subscriptionservice.subscription.service;

import com.xtended.subscriptionservice.subscription.dto.SubscriptionEvent;
import com.xtended.subscriptionservice.subscription.messaging.OutboxFallbackEventPublisher;
import com.xtended.subscriptionservice.subscription.model.*;
import com.xtended.subscriptionservice.subscription.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для работы с subscription
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final UserService userService;
    private final OutboxFallbackEventPublisher eventPublisher;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Активация подписки
     *
     * @param userId         идентификатор пользователя
     * @param type           тип подписки
     * @param activationDate дата активации подписки
     * @return созданная подписка
     */
    @Transactional
    public Subscription activateSubscription(UUID userId, SubscriptionType type, LocalDate activationDate) {
        User user = userService.createAndReturnUserById(userId);

        if (activationDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Activation date must be in the future or today");
        }

        // Проверка активной подписки (избыточно с БД-ограничением, но даёт читаемое исключение)
        subscriptionRepository.findTopByUserIdAndActiveTrue(userId).ifPresent(s -> {
            throw new IllegalStateException("User already has an active subscription");
        });

        Subscription subscription = Subscription.builder()
                .user(user)
                .type(type)
                .activationDate(activationDate)
                .active(true)
                .build();
        subscription = subscriptionRepository.save(subscription);

        eventPublisher.publishAfterCommit("subscription.activated.queue",
                new SubscriptionEvent(subscription), subscription.getId().toString());

        return subscription;
    }

    /**
     * Деактивация подписки
     *
     * @param userId идентификатор пользователя
     * @param type   тип подписки
     */
    @Transactional
    public void deactivateSubscription(UUID userId, SubscriptionType type) {
        Subscription subscription = subscriptionRepository.findTopByUserIdAndActiveTrue(userId)
                .filter(s -> s.getType() == type)
                .orElseThrow(() -> new EntityNotFoundException("Active subscription not found"));

        subscription.setActive(false);
        subscription.setDeactivationDate(LocalDate.now());
        subscriptionRepository.save(subscription);

        eventPublisher.publishAfterCommit("subscription.deactivated.queue",
                new SubscriptionEvent(subscription), subscription.getId().toString());
    }

    public Optional<Subscription> getActiveSubscription(UUID userId) {
        return subscriptionRepository.findTopByUserIdAndActiveTrue(userId);
    }
}
