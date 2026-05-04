package com.xtended.subscriptionservice.subscription.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Модель счета
 */
@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {
    /**
     * Идентификатор счета
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Пользователь, которому присвоен счет
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Подписка, связанная с счетом
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    /**
     * Дата выдачи счета
     */
    @NonNull
    @Column(nullable = false)
    private LocalDate issueDate;

    /**
     * Сумма в счете
     */
    @NonNull
    @Column(nullable = false)
    private BigDecimal amount;

    /**
     * Тип подписки
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NonNull
    private SubscriptionType subscriptionType;

    /**
     * Дата активации подписки
     */
    @NonNull
    @Column(nullable = false)
    private LocalDate subscriptionActivationDate;

    /**
     * Дата создания записи
     */
    @CreationTimestamp
    private LocalDateTime createdAt;
}
