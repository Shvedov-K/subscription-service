package com.xtended.subscriptionservice.subscription.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Модель подписки
 */
@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {
    /**
     * Идентификатор подписки
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Пользователь, которому присвоена подписка
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Тип подписки
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionType type;

    /**
     * Дата активации подписки
     */
    @Column(nullable = false)
    private LocalDate activationDate;

    /**
     * Флаг активности подписки
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Дата деактивации подписки
     */
    private LocalDate deactivationDate;

    /**
     * Дата активации подписки
     */
    @CreationTimestamp
    private LocalDateTime createdAt;
}