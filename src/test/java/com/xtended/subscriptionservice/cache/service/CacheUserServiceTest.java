package com.xtended.subscriptionservice.cache.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtended.subscriptionservice.cache.model.UserCacheInfo;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для CacheUserService")
class CacheUserServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private CacheUserService cacheUserService;

    private UUID testUserId;
    private SubscriptionEvent testSubscriptionEvent;
    private InvoiceEvent testInvoiceEvent;
    private Pageable testPageable;

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
    }

    @Test
    @DisplayName("Добавление активной подписки в кэш")
    void addActiveSubscription_ValidData_ShouldStoreInCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheUserService.addActiveSubscription(testUserId, testSubscriptionEvent);

        String expectedKey = "user:" + testUserId + ":subscription";
        verify(valueOperations, times(1)).set(expectedKey, testSubscriptionEvent);
    }

    @Test
    @DisplayName("Удаление подписки из кэша")
    void removeSubscription_ValidUserId_ShouldDeleteFromCache() {
        cacheUserService.removeSubscription(testUserId);

        String expectedKey = "user:" + testUserId + ":subscription";
        verify(redisTemplate, times(1)).delete(expectedKey);
    }

    @Test
    @DisplayName("Добавление счета в кэш с валидными данными")
    void addInvoice_ValidData_ShouldStoreInCache() throws JsonProcessingException {
        String jsonEvent = "{\"invoiceId\":\"" + testInvoiceEvent.getInvoiceId() + "\"}";
        when(objectMapper.writeValueAsString(testInvoiceEvent)).thenReturn(jsonEvent);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        cacheUserService.addInvoice(testUserId, testInvoiceEvent);

        String expectedKey = "user:" + testUserId + ":invoices";
        verify(objectMapper, times(1)).writeValueAsString(testInvoiceEvent);
        verify(zSetOperations, times(1)).add(eq(expectedKey), eq(jsonEvent), anyDouble());
    }

    @Test
    @DisplayName("Добавление счета в кэш с ошибкой JSON")
    void addInvoice_JsonProcessingError_ShouldThrowRuntimeException() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(testInvoiceEvent))
            .thenThrow(new JsonProcessingException("JSON error") {});

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            cacheUserService.addInvoice(testUserId, testInvoiceEvent)
        );

        assertInstanceOf(JsonProcessingException.class, exception.getCause());
        verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    @DisplayName("Получение информации о пользователе с активной подпиской и счетами")
    void getUserCacheInfo_WithSubscriptionAndInvoices_ShouldReturnCompleteInfo() 
            throws JsonProcessingException {
        String invoiceJson1 = "{\"invoiceId\":\"test-invoice-1\"}";
        String invoiceJson2 = "{\"invoiceId\":\"test-invoice-2\"}";
        
        when(valueOperations.get("user:" + testUserId + ":subscription"))
            .thenReturn(testSubscriptionEvent);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(zSetOperations.zCard("user:" + testUserId + ":invoices"))
            .thenReturn(2L);
        when(zSetOperations.reverseRange("user:" + testUserId + ":invoices", 0, 9))
            .thenReturn(Set.of(invoiceJson1, invoiceJson2));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(objectMapper.readValue(invoiceJson1, InvoiceEvent.class))
            .thenReturn(testInvoiceEvent);
        when(objectMapper.readValue(invoiceJson2, InvoiceEvent.class))
            .thenReturn(testInvoiceEvent);

        UserCacheInfo result = cacheUserService.getUserCacheInfo(testUserId, testPageable);

        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(testSubscriptionEvent, result.getActiveSubscription());
        
        Page<InvoiceEvent> invoices = result.getInvoices();
        assertNotNull(invoices);
        assertEquals(2, invoices.getContent().size());
        assertEquals(testInvoiceEvent, invoices.getContent().get(0));
        assertEquals(testInvoiceEvent, invoices.getContent().get(1));
        assertEquals(2L, invoices.getTotalElements());
    }

    @Test
    @DisplayName("Получение информации о пользователе без активной подписки")
    void getUserCacheInfo_WithoutSubscription_ShouldReturnNullSubscription() {
        when(valueOperations.get("user:" + testUserId + ":subscription"))
            .thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(zSetOperations.zCard("user:" + testUserId + ":invoices"))
            .thenReturn(0L);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange("user:" + testUserId + ":invoices", 0, 9))
            .thenReturn(Set.of());

        UserCacheInfo result = cacheUserService.getUserCacheInfo(testUserId, testPageable);

        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertNull(result.getActiveSubscription());
        assertTrue(result.getInvoices().getContent().isEmpty());
        assertEquals(0L, result.getInvoices().getTotalElements());
    }

    @Test
    @DisplayName("Получение информации о пользователе с нулевым количеством счетов")
    void getUserCacheInfo_WithNullInvoiceCount_ShouldReturnZeroCount() {
        when(valueOperations.get("user:" + testUserId + ":subscription"))
            .thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(zSetOperations.zCard("user:" + testUserId + ":invoices"))
            .thenReturn(null);
        when(zSetOperations.reverseRange("user:" + testUserId + ":invoices", 0, 9))
            .thenReturn(Set.of());
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        UserCacheInfo result = cacheUserService.getUserCacheInfo(testUserId, testPageable);

        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(0L, result.getInvoices().getTotalElements());
    }

    @Test
    @DisplayName("Получение информации о пользователе с ошибкой JSON при чтении счетов")
    void getUserCacheInfo_JsonReadError_ShouldThrowRuntimeException() 
            throws JsonProcessingException {
        String invalidJson = "invalid json";
        
        when(valueOperations.get("user:" + testUserId + ":subscription"))
            .thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(zSetOperations.zCard("user:" + testUserId + ":invoices"))
            .thenReturn(1L);
        when(zSetOperations.reverseRange("user:" + testUserId + ":invoices", 0, 9))
            .thenReturn(Set.of(invalidJson));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(objectMapper.readValue(invalidJson, InvoiceEvent.class))
            .thenThrow(new JsonProcessingException("JSON error") {});

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            cacheUserService.getUserCacheInfo(testUserId, testPageable)
        );

        assertEquals("JSON error", exception.getMessage());
    }

    @Test
    @DisplayName("Проверка правильного формирования ключей для кэша")
    void addActiveSubscription_ShouldUseCorrectKeyFormat() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheUserService.addActiveSubscription(testUserId, testSubscriptionEvent);

        String expectedKey = "user:" + testUserId + ":subscription";
        verify(valueOperations, times(1)).set(eq(expectedKey), eq(testSubscriptionEvent));
    }

    @Test
    @DisplayName("Проверка правильного формирования ключей для счетов")
    void addInvoice_ShouldUseCorrectKeyFormat() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(testInvoiceEvent)).thenReturn("{}");
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        cacheUserService.addInvoice(testUserId, testInvoiceEvent);

        String expectedKey = "user:" + testUserId + ":invoices";
        verify(zSetOperations, times(1)).add(eq(expectedKey), anyString(), anyDouble());
    }
}
