package com.xtended.subscriptionservice.cache.service;

import com.xtended.subscriptionservice.cache.dto.InvoicesPageResponse;
import com.xtended.subscriptionservice.subscription.dto.ApiResponse;
import com.xtended.subscriptionservice.subscription.dto.InvoiceEvent;
import com.xtended.subscriptionservice.subscription.dto.SubscriptionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

/**
 * Реализация fallback-клиента для сервиса подписок
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceFallbackClient {

    private static final String USER_ACTIVE_SUBSCRIPTION = "/api/v1/subscriptions/%s/active";
    private static final String USER_INVOICES = "/api/v1/subscriptions/%s/invoices?page=%s&size=%s";

    private final RestTemplate restTemplate;
    private final String subscriptionServiceBaseUrl;

    /**
     * Получение активной подписки пользователя
     *
     * @param userId ID пользователя
     * @return активная подписка пользователя
     */
    public SubscriptionEvent getActiveSubscription(UUID userId) {
        String url = subscriptionServiceBaseUrl + USER_ACTIVE_SUBSCRIPTION.formatted(userId);
        try {
            ResponseEntity<ApiResponse<SubscriptionEvent>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            return response.getBody() != null ? response.getBody().getData() : null;
        } catch (Exception e) {
            log.error("Failed to fetch active subscription from main service", e);
            throw e;
        }
    }

    /**
     * Получение счетов пользователя
     *
     * @param userId   ID пользователя
     * @param pageable параметры пагинации
     * @return список счетов пользователя
     */
    public List<InvoiceEvent> getInvoices(UUID userId, Pageable pageable) {
        String url = subscriptionServiceBaseUrl +
                USER_INVOICES.formatted(userId, pageable.getPageNumber(), pageable.getPageSize());
        try {
            ResponseEntity<ApiResponse<InvoicesPageResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            return response.getBody() != null && response.getBody().getData() != null 
                ? response.getBody().getData().getContent() : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch invoices from main service", e);
            throw e;
        }
    }
}
