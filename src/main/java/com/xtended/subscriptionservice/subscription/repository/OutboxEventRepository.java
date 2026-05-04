package com.xtended.subscriptionservice.subscription.repository;

import com.xtended.subscriptionservice.subscription.model.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Репозитарий для работы с outbox
 */
public interface OutboxEventRepository extends JpaRepository<Outbox, UUID> {
}
