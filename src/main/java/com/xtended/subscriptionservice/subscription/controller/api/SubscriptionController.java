package com.xtended.subscriptionservice.subscription.controller.api;

import com.xtended.subscriptionservice.subscription.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public interface SubscriptionController {

    /**
     * Активация подписки
     *
     * @param request запрос активации подписки
     * @return ответ
     */
    @PostMapping("/activate")
    ResponseEntity<ApiResponse<UUID>> activate(@Valid @RequestBody ActivateSubscriptionRequest request);

    /**
     * Деактивация подписки
     *
     * @param request запрос деактивации подписки
     * @return ответ
     */
    @PostMapping("/deactivate")
    ResponseEntity<ApiResponse<UUID>> deactivate(@Valid @RequestBody DeactivateSubscriptionRequest request);

    /**
     * Получение активной подписки пользователя
     *
     * @param userId ID пользователя
     * @return ответ
     */
    @GetMapping("/{userId}/active")
    ResponseEntity<ApiResponse<SubscriptionEvent>> getActive(@PathVariable UUID userId);

    /**
     * Получить список счетов пользователя
     *
     * @param userId ID пользователя
     * @param pageable параметры пагинации
     * @return ответ
     */
    @GetMapping("/{userId}/invoices")
    ResponseEntity<ApiResponse<Page<InvoiceEvent>>> getInvoices(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable);
}
