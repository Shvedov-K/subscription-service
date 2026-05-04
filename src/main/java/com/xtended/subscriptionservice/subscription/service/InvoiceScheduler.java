package com.xtended.subscriptionservice.subscription.service;

import com.xtended.subscriptionservice.subscription.model.Subscription;
import com.xtended.subscriptionservice.subscription.repository.InvoiceRepository;
import com.xtended.subscriptionservice.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceScheduler {
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    @Scheduled(cron = "0 0 2 * * *") // каждый день в 2 часа ночи
    public void issueMonthlyInvoices() {
        List<Subscription> activeSubscriptions = subscriptionRepository.findAllByActiveTrue();
        LocalDate today = LocalDate.now();
        for (Subscription sub : activeSubscriptions) {

            // Пропускаем, если сегодня меньше дня активации
            if (today.isBefore(sub.getActivationDate())) continue;

            // Выставляем счёт, если сегодня совпадает день месяца с днём активации
            if (today.getDayOfMonth() == sub.getActivationDate().getDayOfMonth()) {
                // Проверяем, не был ли уже выставлен счёт за этот месяц
                if (!invoiceRepository.existsBySubscriptionAndIssueDate(sub, today)) {
                    invoiceService.issueInvoice(sub, today);   // метод должен быть @Transactional
                }
            }
        }
    }
}
