package com.xtended.subscriptionservice.cache.controller;

import com.xtended.subscriptionservice.cache.model.UserCacheInfo;
import com.xtended.subscriptionservice.cache.service.CacheUserService;
import com.xtended.subscriptionservice.cache.service.SubscriptionServiceFallbackClient;
import com.xtended.subscriptionservice.subscription.dto.ApiResponse;
import com.xtended.subscriptionservice.subscription.dto.InvoiceEvent;
import com.xtended.subscriptionservice.subscription.dto.SubscriptionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Контроллер для работы с кэшем пользователей
 */
@RestController
@RequestMapping("/api/v1/cache/users")
@RequiredArgsConstructor
@Slf4j
public class CacheController {

    private final CacheUserService cacheUserService;
    private final SubscriptionServiceFallbackClient fallbackClient;

    /**
     * Получение информации о пользователе из кэша.
     * Если redis недоступен, то информация будет получена из основного сервиса через rest.
     *
     * @param userId   ID пользователя
     * @param pageable параметры пагинации
     * @return информация о пользователе
     */
    @GetMapping("/{userId}/info")
    public ResponseEntity<ApiResponse<UserCacheInfo>> getUserInfo(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        try {
            UserCacheInfo info = cacheUserService.getUserCacheInfo(userId, pageable);
            return ResponseEntity.ok(new ApiResponse<>(true, "OK", info));
        } catch (Exception e) {
            log.warn("Redis unavailable, falling back to main service for user {}", userId, e);
            SubscriptionEvent activeSub = fallbackClient.getActiveSubscription(userId);
            List<InvoiceEvent> invoices = fallbackClient.getInvoices(userId, pageable);

            PageImpl<InvoiceEvent> invoicePage = new PageImpl<>(
                    new ArrayList<>(invoices),
                    pageable,
                    invoices.size()
            );

            UserCacheInfo fallbackInfo = new UserCacheInfo(userId, activeSub, invoicePage);
            return ResponseEntity.ok(new ApiResponse<>(true, "OK", fallbackInfo));
        }
    }
}

