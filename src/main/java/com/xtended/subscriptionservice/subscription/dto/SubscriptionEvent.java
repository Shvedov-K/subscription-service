package com.xtended.subscriptionservice.subscription.dto;

import com.xtended.subscriptionservice.subscription.model.Subscription;
import com.xtended.subscriptionservice.subscription.model.SubscriptionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEvent implements Serializable {

    public SubscriptionEvent(Subscription subscription) {
        this.userId = subscription.getUser().getId();
        this.activationDate = subscription.getActivationDate();
        this.subscriptionId = subscription.getId();
        this.type = subscription.getType();
    }

    private UUID subscriptionId;
    private UUID userId;
    private SubscriptionType type;
    private LocalDate activationDate;
}
