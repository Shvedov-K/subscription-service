package com.xtended.subscriptionservice.subscription.repository;

import com.xtended.subscriptionservice.subscription.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозитарий для работы с subscription
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    /**
     * Находит первую активную подписку пользователя
     * @param userId идентификатор пользователя
     * @return первая активная подписка пользователя или пустой Optional
     */
    Optional<Subscription> findTopByUserIdAndActiveTrue(UUID userId);

    /**
     * Находит все активные подписки
     * @return список активных подписок
     */
    List<Subscription> findAllByActiveTrue();
}
