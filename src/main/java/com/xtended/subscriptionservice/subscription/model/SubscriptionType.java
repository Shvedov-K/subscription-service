package com.xtended.subscriptionservice.subscription.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Тип подписки
 */
@Getter
@RequiredArgsConstructor
public enum SubscriptionType {
    BASIC("Basic", new BigDecimal("100.00")),
    PRO("PRO", new BigDecimal("200.00"));

    private final String displayName;
    private final BigDecimal price;
}
