package com.xtended.subscriptionservice.cache.controller;

import com.xtended.subscriptionservice.cache.controller.impl.CacheControllerImpl;
import com.xtended.subscriptionservice.cache.model.UserCacheInfo;
import com.xtended.subscriptionservice.cache.service.CacheUserService;
import com.xtended.subscriptionservice.cache.service.SubscriptionServiceFallbackClient;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для CacheController")
class CacheControllerTest {

    @Mock
    private CacheUserService cacheUserService;

    @Mock
    private SubscriptionServiceFallbackClient fallbackClient;

    @InjectMocks
    private CacheControllerImpl cacheController;

    private UUID testUserId;
    private SubscriptionEvent testSubscriptionEvent;
    private InvoiceEvent testInvoiceEvent;
    private Pageable testPageable;
    private UserCacheInfo testUserCacheInfo;

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
        
        Page<InvoiceEvent> invoicePage = new PageImpl<>(
            List.of(testInvoiceEvent),
            testPageable,
            1
        );
        
        testUserCacheInfo = new UserCacheInfo(testUserId, testSubscriptionEvent, invoicePage);
    }

    @Test
    @DisplayName("Успешное получение информации о пользователе из кэша")
    void getUserInfo_SuccessFromCache_ShouldReturnApiResponse() {
        when(cacheUserService.getUserCacheInfo(testUserId, testPageable))
            .thenReturn(testUserCacheInfo);

        ResponseEntity<ApiResponse<UserCacheInfo>> response = 
            cacheController.getUserInfo(testUserId, testPageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("OK", response.getBody().getMessage());
        assertEquals(testUserCacheInfo, response.getBody().getData());
        
        verify(cacheUserService, times(1)).getUserCacheInfo(testUserId, testPageable);
        verify(fallbackClient, never()).getActiveSubscription(any());
        verify(fallbackClient, never()).getInvoices(any(), any());
    }

    @Test
    @DisplayName("Получение информации о пользователе с fallback при ошибке кэша")
    void getUserInfo_CacheError_ShouldUseFallback() {
        RuntimeException cacheException = new RuntimeException("Redis unavailable");
        when(cacheUserService.getUserCacheInfo(testUserId, testPageable))
            .thenThrow(cacheException);
        
        when(fallbackClient.getActiveSubscription(testUserId))
            .thenReturn(testSubscriptionEvent);
        
        List<InvoiceEvent> fallbackInvoices = List.of(testInvoiceEvent);
        when(fallbackClient.getInvoices(testUserId, testPageable))
            .thenReturn(fallbackInvoices);

        ResponseEntity<ApiResponse<UserCacheInfo>> response = 
            cacheController.getUserInfo(testUserId, testPageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("OK", response.getBody().getMessage());
        
        UserCacheInfo resultData = response.getBody().getData();
        assertEquals(testUserId, resultData.getUserId());
        assertEquals(testSubscriptionEvent, resultData.getActiveSubscription());
        assertEquals(1, resultData.getInvoices().getTotalElements());
        assertEquals(testInvoiceEvent, resultData.getInvoices().getContent().get(0));
        
        verify(cacheUserService, times(1)).getUserCacheInfo(testUserId, testPageable);
        verify(fallbackClient, times(1)).getActiveSubscription(testUserId);
        verify(fallbackClient, times(1)).getInvoices(testUserId, testPageable);
    }

    @Test
    @DisplayName("Fallback сценарий с пустыми данными от основного сервиса")
    void getUserInfo_FallbackWithEmptyData_ShouldHandleGracefully() {
        when(cacheUserService.getUserCacheInfo(testUserId, testPageable))
            .thenThrow(new RuntimeException("Redis error"));
        
        when(fallbackClient.getActiveSubscription(testUserId))
            .thenReturn(null);
        
        when(fallbackClient.getInvoices(testUserId, testPageable))
            .thenReturn(List.of());

        ResponseEntity<ApiResponse<UserCacheInfo>> response = 
            cacheController.getUserInfo(testUserId, testPageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        
        UserCacheInfo resultData = response.getBody().getData();
        assertEquals(testUserId, resultData.getUserId());
        assertNull(resultData.getActiveSubscription());
        assertTrue(resultData.getInvoices().getContent().isEmpty());
        assertEquals(0, resultData.getInvoices().getTotalElements());
        
        verify(fallbackClient, times(1)).getActiveSubscription(testUserId);
        verify(fallbackClient, times(1)).getInvoices(testUserId, testPageable);
    }

    @Test
    @DisplayName("Проверка работы с разными параметрами пагинации")
    void getUserInfo_DifferentPageable_ShouldPassCorrectParameters() {
        Pageable differentPageable = PageRequest.of(2, 5);
        
        Page<InvoiceEvent> differentInvoicePage = new PageImpl<>(
            List.of(testInvoiceEvent),
            differentPageable,
            1
        );
        
        UserCacheInfo differentCacheInfo = new UserCacheInfo(
            testUserId, 
            testSubscriptionEvent, 
            differentInvoicePage
        );
        
        when(cacheUserService.getUserCacheInfo(testUserId, differentPageable))
            .thenReturn(differentCacheInfo);

        ResponseEntity<ApiResponse<UserCacheInfo>> response = 
            cacheController.getUserInfo(testUserId, differentPageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(differentCacheInfo, response.getBody().getData());
        
        verify(cacheUserService, times(1)).getUserCacheInfo(testUserId, differentPageable);
    }

    @Test
    @DisplayName("Fallback сценарий с несколькими счетами")
    void getUserInfo_FallbackWithMultipleInvoices_ShouldCreateCorrectPage() {
        InvoiceEvent secondInvoice = InvoiceEvent.builder()
            .invoiceId(UUID.randomUUID())
            .userId(testUserId)
            .invoiceDate(LocalDate.now().minusDays(1))
            .amount(new BigDecimal("50.00"))
            .subscriptionName("BASIC")
            .subscriptionActivationDate(LocalDate.now().minusDays(60))
            .build();
        
        when(cacheUserService.getUserCacheInfo(testUserId, testPageable))
            .thenThrow(new RuntimeException("Redis unavailable"));
        
        when(fallbackClient.getActiveSubscription(testUserId))
            .thenReturn(testSubscriptionEvent);
        
        List<InvoiceEvent> multipleInvoices = List.of(testInvoiceEvent, secondInvoice);
        when(fallbackClient.getInvoices(testUserId, testPageable))
            .thenReturn(multipleInvoices);

        ResponseEntity<ApiResponse<UserCacheInfo>> response = 
            cacheController.getUserInfo(testUserId, testPageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        UserCacheInfo resultData = response.getBody().getData();
        assertEquals(testUserId, resultData.getUserId());
        assertEquals(testSubscriptionEvent, resultData.getActiveSubscription());
        assertEquals(2, resultData.getInvoices().getContent().size());
        assertEquals(2, resultData.getInvoices().getTotalElements());
        
        verify(fallbackClient, times(1)).getInvoices(testUserId, testPageable);
    }

    @Test
    @DisplayName("Проверка структуры ответа API")
    void getUserInfo_ResponseStructure_ShouldMatchExpectedFormat() {
        when(cacheUserService.getUserCacheInfo(testUserId, testPageable))
            .thenReturn(testUserCacheInfo);

        ResponseEntity<ApiResponse<UserCacheInfo>> response = 
            cacheController.getUserInfo(testUserId, testPageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse<UserCacheInfo> apiResponse = response.getBody();
        
        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals("OK", apiResponse.getMessage());
        assertNotNull(apiResponse.getData());
        assertNotNull(apiResponse.getTimestamp());
        assertEquals(testUserCacheInfo, apiResponse.getData());
    }

    @Test
    @DisplayName("Fallback с исключением при вызове основного сервиса")
    void getUserInfo_FallbackServiceException_ShouldPropagateException() {
        when(cacheUserService.getUserCacheInfo(testUserId, testPageable))
            .thenThrow(new RuntimeException("Redis error"));
        
        when(fallbackClient.getActiveSubscription(testUserId))
            .thenThrow(new RuntimeException("Main service unavailable"));

        assertThrows(RuntimeException.class, () -> 
            cacheController.getUserInfo(testUserId, testPageable)
        );
        
        verify(cacheUserService, times(1)).getUserCacheInfo(testUserId, testPageable);
        verify(fallbackClient, times(1)).getActiveSubscription(testUserId);
        verify(fallbackClient, never()).getInvoices(any(), any());
    }

    @Test
    @DisplayName("Проверка вызова с null userId")
    void getUserInfo_NullUserId_ShouldHandleGracefully() {
        assertThrows(NullPointerException.class, () ->
            cacheController.getUserInfo(null, testPageable)
        );
        
        verify(cacheUserService, never()).getUserCacheInfo(isNull(), eq(testPageable));
    }
}
