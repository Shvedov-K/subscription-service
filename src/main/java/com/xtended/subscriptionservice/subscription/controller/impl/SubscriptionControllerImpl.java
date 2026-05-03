package com.xtended.subscriptionservice.subscription.controller.impl;

import com.xtended.subscriptionservice.subscription.controller.api.SubscriptionController;
import com.xtended.subscriptionservice.subscription.dto.*;
import com.xtended.subscriptionservice.subscription.model.Invoice;
import com.xtended.subscriptionservice.subscription.model.Subscription;
import com.xtended.subscriptionservice.subscription.service.InvoiceService;
import com.xtended.subscriptionservice.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionControllerImpl implements SubscriptionController {

    private final InvoiceService invoiceService;
    private final SubscriptionService subscriptionService;

    public ResponseEntity<ApiResponse<UUID>> activate(ActivateSubscriptionRequest request) {
        Subscription subscription = subscriptionService.activateSubscription(
                request.getUserId(), request.getSubscriptionType(), request.getActivationDate());

        invoiceService.issueInvoice(subscription, subscription.getActivationDate());

        return ResponseEntity.ok(new ApiResponse<>(true, "OK", request.getUserId()));
    }

    public ResponseEntity<ApiResponse<UUID>> deactivate(DeactivateSubscriptionRequest request) {
        subscriptionService.deactivateSubscription(request.getUserId(), request.getSubscriptionType());
        return ResponseEntity.ok(new ApiResponse<>(true, "OK", request.getUserId()));
    }

    public ResponseEntity<ApiResponse<SubscriptionEvent>> getActive(@PathVariable UUID userId) {
        Subscription subscription = subscriptionService.getActiveSubscription(userId).orElseThrow();
        return ResponseEntity.ok(new ApiResponse<>(true, "OK", new SubscriptionEvent(subscription)));
    }

    public ResponseEntity<ApiResponse<Page<InvoiceEvent>>> getInvoices(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Invoice> page = invoiceService.getUserInvoices(userId, pageable);
        Page<InvoiceEvent> dtoPage = page.map(this::toDto);
        return ResponseEntity.ok(new ApiResponse<>(true, "OK", dtoPage));
    }

    private InvoiceEvent toDto(Invoice invoice) {
        return new InvoiceEvent(invoice);
    }
}
