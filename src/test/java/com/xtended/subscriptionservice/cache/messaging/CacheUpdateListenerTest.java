package com.xtended.subscriptionservice.cache.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtended.subscriptionservice.cache.service.CacheUserService;
import com.xtended.subscriptionservice.subscription.dto.InvoiceEvent;
import com.xtended.subscriptionservice.subscription.dto.SubscriptionEvent;
import com.xtended.subscriptionservice.subscription.model.SubscriptionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для CacheUpdateListener")
class CacheUpdateListenerTest {

    @Mock
    private CacheUserService cacheUserService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CacheUpdateListener cacheUpdateListener;

    private UUID userId;
    private SubscriptionEvent subscriptionEvent;
    private InvoiceEvent invoiceEvent;
    private String subscriptionJson;
    private String invoiceJson;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        subscriptionEvent = new SubscriptionEvent();
        subscriptionEvent.setSubscriptionId(subscriptionId);
        subscriptionEvent.setUserId(userId);
        subscriptionEvent.setType(SubscriptionType.PRO);
        subscriptionEvent.setActivationDate(LocalDate.now());

        invoiceEvent = InvoiceEvent.builder()
                .invoiceId(invoiceId)
                .userId(userId)
                .invoiceDate(LocalDate.now())
                .amount(new BigDecimal("99.99"))
                .subscriptionName("PREMIUM")
                .subscriptionActivationDate(LocalDate.now().minusDays(1))
                .build();

        subscriptionJson = "{\"subscriptionId\":\"" + subscriptionId + "\",\"userId\":\"" + userId + "\"}";
        invoiceJson = "{\"invoiceId\":\"" + invoiceId + "\",\"userId\":\"" + userId + "\"}";
    }

    @Test
    @DisplayName("Должен успешно обработать событие активации подписки")
    void handleSubscriptionActivated_shouldProcessSuccessfully() throws Exception {
        when(objectMapper.readValue(subscriptionJson, SubscriptionEvent.class))
                .thenReturn(subscriptionEvent);

        cacheUpdateListener.handleSubscriptionActivated(subscriptionJson);

        verify(cacheUserService, times(1)).addActiveSubscription(userId, subscriptionEvent);
        verify(objectMapper, times(1)).readValue(subscriptionJson, SubscriptionEvent.class);
    }

    @Test
    @DisplayName("Должен успешно обработать событие деактивации подписки")
    void handleSubscriptionDeactivated_shouldProcessSuccessfully() throws Exception {
        when(objectMapper.readValue(subscriptionJson, SubscriptionEvent.class))
                .thenReturn(subscriptionEvent);

        cacheUpdateListener.handleSubscriptionDeactivated(subscriptionJson);

        verify(cacheUserService, times(1)).removeSubscription(userId);
        verify(objectMapper, times(1)).readValue(subscriptionJson, SubscriptionEvent.class);
    }

    @Test
    @DisplayName("Должен успешно обработать событие выставленного счета")
    void handleInvoiceIssued_shouldProcessSuccessfully() throws Exception {
        when(objectMapper.readValue(invoiceJson, InvoiceEvent.class))
                .thenReturn(invoiceEvent);

        cacheUpdateListener.handleInvoiceIssued(invoiceJson);

        verify(cacheUserService, times(1)).addInvoice(userId, invoiceEvent);
        verify(objectMapper, times(1)).readValue(invoiceJson, InvoiceEvent.class);
    }

    @Test
    @DisplayName("Должен обработать ошибку при парсинге JSON в handleSubscriptionActivated")
    void handleSubscriptionActivated_shouldHandleJsonParsingError() throws Exception {
        String invalidJson = "invalid json";
        when(objectMapper.readValue(invalidJson, SubscriptionEvent.class))
                .thenThrow(new RuntimeException("JSON parsing error"));

        cacheUpdateListener.handleSubscriptionActivated(invalidJson);

        verify(cacheUserService, never()).addActiveSubscription(any(), any());
        verify(objectMapper, times(1)).readValue(invalidJson, SubscriptionEvent.class);
    }

    @Test
    @DisplayName("Должен обработать ошибку при парсинге JSON в handleSubscriptionDeactivated")
    void handleSubscriptionDeactivated_shouldHandleJsonParsingError() throws Exception {
        String invalidJson = "invalid json";
        when(objectMapper.readValue(invalidJson, SubscriptionEvent.class))
                .thenThrow(new RuntimeException("JSON parsing error"));

        cacheUpdateListener.handleSubscriptionDeactivated(invalidJson);

        verify(cacheUserService, never()).removeSubscription(any());
        verify(objectMapper, times(1)).readValue(invalidJson, SubscriptionEvent.class);
    }

    @Test
    @DisplayName("Должен обработать ошибку при парсинге JSON в handleInvoiceIssued")
    void handleInvoiceIssued_shouldHandleJsonParsingError() throws Exception {
        String invalidJson = "invalid json";
        when(objectMapper.readValue(invalidJson, InvoiceEvent.class))
                .thenThrow(new RuntimeException("JSON parsing error"));

        cacheUpdateListener.handleInvoiceIssued(invalidJson);

        verify(cacheUserService, never()).addInvoice(any(), any());
        verify(objectMapper, times(1)).readValue(invalidJson, InvoiceEvent.class);
    }

    @Test
    @DisplayName("Должен обработать ошибку сервиса в handleSubscriptionActivated")
    void handleSubscriptionActivated_shouldHandleServiceError() throws Exception {
        when(objectMapper.readValue(subscriptionJson, SubscriptionEvent.class))
                .thenReturn(subscriptionEvent);
        doThrow(new RuntimeException("Service error"))
                .when(cacheUserService).addActiveSubscription(userId, subscriptionEvent);

        cacheUpdateListener.handleSubscriptionActivated(subscriptionJson);

        verify(objectMapper, times(1)).readValue(subscriptionJson, SubscriptionEvent.class);
        verify(cacheUserService, times(1)).addActiveSubscription(userId, subscriptionEvent);
    }

    @Test
    @DisplayName("Должен обработать ошибку сервиса в handleSubscriptionDeactivated")
    void handleSubscriptionDeactivated_shouldHandleServiceError() throws Exception {
        when(objectMapper.readValue(subscriptionJson, SubscriptionEvent.class))
                .thenReturn(subscriptionEvent);
        doThrow(new RuntimeException("Service error"))
                .when(cacheUserService).removeSubscription(userId);

        cacheUpdateListener.handleSubscriptionDeactivated(subscriptionJson);

        verify(objectMapper, times(1)).readValue(subscriptionJson, SubscriptionEvent.class);
        verify(cacheUserService, times(1)).removeSubscription(userId);
    }

    @Test
    @DisplayName("Должен обработать ошибку сервиса в handleInvoiceIssued")
    void handleInvoiceIssued_shouldHandleServiceError() throws Exception {
        when(objectMapper.readValue(invoiceJson, InvoiceEvent.class))
                .thenReturn(invoiceEvent);
        doThrow(new RuntimeException("Service error"))
                .when(cacheUserService).addInvoice(userId, invoiceEvent);

        cacheUpdateListener.handleInvoiceIssued(invoiceJson);

        verify(objectMapper, times(1)).readValue(invoiceJson, InvoiceEvent.class);
        verify(cacheUserService, times(1)).addInvoice(userId, invoiceEvent);
    }
}
