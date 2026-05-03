package com.xtended.subscriptionservice.subscription.dto;

import com.xtended.subscriptionservice.subscription.model.Invoice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceEvent implements Serializable {

    public InvoiceEvent(Invoice invoice) {
        this.invoiceId = invoice.getId();
        this.userId = invoice.getUser().getId();
        this.invoiceDate = invoice.getIssueDate();
        this.amount = invoice.getAmount();
        this.subscriptionName = invoice.getSubscription().getType().name();
        this.subscriptionActivationDate = invoice.getSubscriptionActivationDate();
    }

    private UUID invoiceId;
    private UUID userId;
    private LocalDate invoiceDate;
    private BigDecimal amount;
    private String subscriptionName;
    private LocalDate subscriptionActivationDate;
}
