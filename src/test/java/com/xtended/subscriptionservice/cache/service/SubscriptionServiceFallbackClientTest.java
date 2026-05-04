package com.xtended.subscriptionservice.cache.service;

import com.xtended.subscriptionservice.cache.dto.InvoicesPageResponse;
import com.xtended.subscriptionservice.subscription.dto.ApiResponse;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для SubscriptionServiceFallbackClient")
class SubscriptionServiceFallbackClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SubscriptionServiceFallbackClient fallbackClient;

    private UUID testUserId;
    private SubscriptionEvent testSubscriptionEvent;
    private InvoiceEvent testInvoiceEvent;
    private Pageable testPageable;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testSubscriptionEvent = new SubscriptionEvent(
            UUID.randomUUID(),
            testUserId,
            SubscriptionType.PRO,
            LocalDate.now()
        );
        
        testInvoiceEvent = InvoiceEvent.builder()
            .invoiceId(UUID.randomUUID())
            .userId(testUserId)
            .invoiceDate(LocalDate.now())
            .amount(new BigDecimal("100.00"))
            .subscriptionName("PRO")
            .subscriptionActivationDate(LocalDate.now().minusDays(30))
            .build();
            
        testPageable = PageRequest.of(0, 10);
        baseUrl = "http://localhost:8080";
        
        fallbackClient = new SubscriptionServiceFallbackClient(restTemplate, baseUrl);
    }

    @Test
    @DisplayName("Получение активной подписки с успешным ответом")
    void getActiveSubscription_SuccessResponse_ShouldReturnSubscription() {
        ApiResponse<SubscriptionEvent> apiResponse = new ApiResponse<>(true, "Success", testSubscriptionEvent);
        ResponseEntity<ApiResponse<SubscriptionEvent>> responseEntity = 
            new ResponseEntity<>(apiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/active"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        SubscriptionEvent result = fallbackClient.getActiveSubscription(testUserId);

        assertNotNull(result);
        assertEquals(testSubscriptionEvent.getSubscriptionId(), result.getSubscriptionId());
        assertEquals(testSubscriptionEvent.getUserId(), result.getUserId());
        assertEquals(testSubscriptionEvent.getType(), result.getType());
        assertEquals(testSubscriptionEvent.getActivationDate(), result.getActivationDate());
    }

    @Test
    @DisplayName("Получение активной подписки с пустым телом ответа")
    void getActiveSubscription_NullBody_ShouldReturnNull() {
        ResponseEntity<ApiResponse<SubscriptionEvent>> responseEntity = 
            ResponseEntity.ok().build();

        when(restTemplate.exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/active"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        SubscriptionEvent result = fallbackClient.getActiveSubscription(testUserId);

        assertNull(result);
    }

    @Test
    @DisplayName("Получение активной подписки с пустыми данными в ответе")
    void getActiveSubscription_NullData_ShouldReturnNull() {
        ApiResponse<SubscriptionEvent> apiResponse = new ApiResponse<>(true, "Success", null);
        ResponseEntity<ApiResponse<SubscriptionEvent>> responseEntity = 
            new ResponseEntity<>(apiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/active"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        SubscriptionEvent result = fallbackClient.getActiveSubscription(testUserId);

        assertNull(result);
    }

    @Test
    @DisplayName("Получение активной подписки с исключением")
    void getActiveSubscription_Exception_ShouldThrowException() {
        when(restTemplate.exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/active"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("Service unavailable"));

        assertThrows(RuntimeException.class, () -> fallbackClient.getActiveSubscription(testUserId));
    }

    @Test
    @DisplayName("Получение счетов с успешным ответом")
    void getInvoices_SuccessResponse_ShouldReturnInvoiceList() {
        List<InvoiceEvent> invoiceList = List.of(testInvoiceEvent);
        InvoicesPageResponse pageResponse = new InvoicesPageResponse(invoiceList, 1, 1);
        ApiResponse<InvoicesPageResponse> apiResponse = new ApiResponse<>(true, "Success", pageResponse);
        ResponseEntity<ApiResponse<InvoicesPageResponse>> responseEntity = 
            new ResponseEntity<>(apiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/invoices?page=0&size=10"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        List<InvoiceEvent> result = fallbackClient.getInvoices(testUserId, testPageable);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testInvoiceEvent.getInvoiceId(), result.get(0).getInvoiceId());
        assertEquals(testInvoiceEvent.getUserId(), result.get(0).getUserId());
        assertEquals(testInvoiceEvent.getAmount(), result.get(0).getAmount());
    }

    @Test
    @DisplayName("Получение счетов с пустым телом ответа")
    void getInvoices_NullBody_ShouldReturnEmptyList() {
        ResponseEntity<ApiResponse<InvoicesPageResponse>> responseEntity = 
            ResponseEntity.ok().build();

        when(restTemplate.exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/invoices?page=0&size=10"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        List<InvoiceEvent> result = fallbackClient.getInvoices(testUserId, testPageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Получение счетов с пустыми данными в ответе")
    void getInvoices_NullData_ShouldReturnEmptyList() {
        ApiResponse<InvoicesPageResponse> apiResponse = new ApiResponse<>(true, "Success", null);
        ResponseEntity<ApiResponse<InvoicesPageResponse>> responseEntity = 
            new ResponseEntity<>(apiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/invoices?page=0&size=10"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        List<InvoiceEvent> result = fallbackClient.getInvoices(testUserId, testPageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Получение счетов с пустым списком в данных")
    void getInvoices_EmptyContent_ShouldReturnEmptyList() {
        InvoicesPageResponse pageResponse = new InvoicesPageResponse(List.of(), 0, 0);
        ApiResponse<InvoicesPageResponse> apiResponse = new ApiResponse<>(true, "Success", pageResponse);
        ResponseEntity<ApiResponse<InvoicesPageResponse>> responseEntity = 
            new ResponseEntity<>(apiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/invoices?page=0&size=10"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        List<InvoiceEvent> result = fallbackClient.getInvoices(testUserId, testPageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Получение счетов с исключением")
    void getInvoices_Exception_ShouldThrowException() {
        when(restTemplate.exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/invoices?page=0&size=10"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("Service unavailable"));

        assertThrows(RuntimeException.class, () -> fallbackClient.getInvoices(testUserId, testPageable));
    }

    @Test
    @DisplayName("Проверка правильного формирования URL для активной подписки")
    void getActiveSubscription_ShouldUseCorrectUrl() {
        ApiResponse<SubscriptionEvent> apiResponse = new ApiResponse<>(true, "Success", testSubscriptionEvent);
        ResponseEntity<ApiResponse<SubscriptionEvent>> responseEntity = 
            new ResponseEntity<>(apiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/active"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        fallbackClient.getActiveSubscription(testUserId);

        verify(restTemplate, times(1)).exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/active"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        );
    }

    @Test
    @DisplayName("Проверка правильного формирования URL для счетов с пагинацией")
    void getInvoices_ShouldUseCorrectUrlWithPagination() {
        List<InvoiceEvent> invoiceList = List.of(testInvoiceEvent);
        InvoicesPageResponse pageResponse = new InvoicesPageResponse(invoiceList, 1, 1);
        ApiResponse<InvoicesPageResponse> apiResponse = new ApiResponse<>(true, "Success", pageResponse);
        ResponseEntity<ApiResponse<InvoicesPageResponse>> responseEntity = 
            new ResponseEntity<>(apiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/invoices?page=0&size=10"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        fallbackClient.getInvoices(testUserId, testPageable);

        verify(restTemplate, times(1)).exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/invoices?page=0&size=10"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        );
    }

    @Test
    @DisplayName("Проверка работы с разными параметрами пагинации")
    void getInvoices_DifferentPagination_ShouldUseCorrectUrl() {
        Pageable differentPageable = PageRequest.of(2, 20);
        List<InvoiceEvent> invoiceList = List.of(testInvoiceEvent);
        InvoicesPageResponse pageResponse = new InvoicesPageResponse(invoiceList, 1, 1);
        ApiResponse<InvoicesPageResponse> apiResponse = new ApiResponse<>(true, "Success", pageResponse);
        ResponseEntity<ApiResponse<InvoicesPageResponse>> responseEntity = 
            new ResponseEntity<>(apiResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/invoices?page=2&size=20"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        List<InvoiceEvent> result = fallbackClient.getInvoices(testUserId, differentPageable);

        assertNotNull(result);
        assertEquals(1, result.size());
        
        verify(restTemplate, times(1)).exchange(
            eq(baseUrl + "/api/v1/subscriptions/" + testUserId + "/invoices?page=2&size=20"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        );
    }
}
