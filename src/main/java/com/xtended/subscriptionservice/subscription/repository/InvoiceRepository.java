package com.xtended.subscriptionservice.subscription.repository;

import com.xtended.subscriptionservice.subscription.model.Invoice;
import com.xtended.subscriptionservice.subscription.model.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Репозитарий для работы с invoice
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    /**
     * Проверяет существование счета по указанной подписке и дате выдачи
     *
     * @param subscription подписка для проверки
     * @param issueDate    дата выдачи счета
     * @return true если счет существует, иначе false
     */
    boolean existsBySubscriptionAndIssueDate(Subscription subscription, LocalDate issueDate);

    Page<Invoice> findByUserIdOrderByIssueDateDesc(UUID userId, Pageable pageable);
}
