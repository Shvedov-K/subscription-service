package com.xtended.subscriptionservice.subscription.dto;

import com.xtended.subscriptionservice.subscription.model.SubscriptionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeactivateSubscriptionRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Subscription type is required")
    private SubscriptionType subscriptionType;
}