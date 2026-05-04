package com.xtended.subscriptionservice.cache.model;

import com.xtended.subscriptionservice.subscription.dto.InvoiceEvent;
import com.xtended.subscriptionservice.subscription.dto.SubscriptionEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Класс, представляющий информацию о пользователе в кэше
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCacheInfo {
    /**
     * ID пользователя
     */
    private UUID userId;
    /**
     * Активная подписка
     */
    private SubscriptionEvent activeSubscription;
    /**
     * Список платежей
     */
    private Page<InvoiceEvent> invoices;
}
