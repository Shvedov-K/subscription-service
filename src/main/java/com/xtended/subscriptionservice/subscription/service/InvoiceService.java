package com.xtended.subscriptionservice.subscription.service;

import com.xtended.subscriptionservice.subscription.dto.InvoiceEvent;
import com.xtended.subscriptionservice.subscription.messaging.OutboxFallbackEventPublisher;
import com.xtended.subscriptionservice.subscription.model.Invoice;
import com.xtended.subscriptionservice.subscription.model.Subscription;
import com.xtended.subscriptionservice.subscription.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Сервис для работы с invoice
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OutboxFallbackEventPublisher eventPublisher;

    /**
     * Создание счета по подписке.
     * Сохранение в базе и отправка в rabbit.
     *
     * @param subscription подписка, для которой создается счет
     * @param issueDate    дата выставления счета
     **/
    @Transactional
    public void issueInvoice(Subscription subscription, LocalDate issueDate) {
        BigDecimal amount = switch (subscription.getType()) {
            case BASIC -> BigDecimal.valueOf(100);
            case PRO -> BigDecimal.valueOf(200);
        };

        Invoice invoice = Invoice.builder()
                .user(subscription.getUser())
                .subscription(subscription)
                .issueDate(issueDate)
                .amount(amount)
                .subscriptionType(subscription.getType())
                .subscriptionActivationDate(subscription.getActivationDate())
                .build();
        invoice = invoiceRepository.save(invoice);

        InvoiceEvent invoiceEvent = new InvoiceEvent(invoice);

        eventPublisher.publishAfterCommit("invoice.issued.queue",
                invoiceEvent, invoice.getId().toString());
        eventPublisher.publishAfterCommit("external.invoice.queue",
                invoiceEvent, invoice.getId().toString());
    }

    /**
     * Получение списка счетов пользователя.
     *
     * @param userId   ID пользователя
     * @param pageable параметры пагинации
     * @return страница счетов пользователя
     */
    public Page<Invoice> getUserInvoices(UUID userId, Pageable pageable) {
        return invoiceRepository.findByUserIdOrderByIssueDateDesc(userId, pageable);
    }
}
