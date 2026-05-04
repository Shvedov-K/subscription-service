package com.xtended.subscriptionservice.cache.controller.api;

import com.xtended.subscriptionservice.cache.model.UserCacheInfo;
import com.xtended.subscriptionservice.subscription.dto.ApiResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Контроллер для работы с кэшем пользователей
 */
@RestController
public interface CacheController {

    /**
     * Получение информации о пользователе из кэша.
     * Если redis недоступен, то информация будет получена из основного сервиса через rest.
     *
     * @param userId   ID пользователя
     * @param pageable параметры пагинации
     * @return информация о пользователе
     */
    @GetMapping("/{userId}/info")
    ResponseEntity<ApiResponse<UserCacheInfo>> getUserInfo(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable);
}

