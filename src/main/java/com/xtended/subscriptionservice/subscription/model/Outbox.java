package com.xtended.subscriptionservice.subscription.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Модель outbox
 */
@Entity
@Table(name = "outbox")
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Getter
public class Outbox {
    /**
     * Идентификатор outbox
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Идентификатор агрегата
     */
    private String aggregateId;
    /**
     * Тип события
     */
    private String eventType;
    /**
     * Ключ маршрутизации
     */
    private String routingKey;

    /**
     * Полезная нагрузка события в формате JSON,
     * содержащая данные для публикации в брокере сообщений
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    /**
     * Дата и время создания записи в outbox
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

}