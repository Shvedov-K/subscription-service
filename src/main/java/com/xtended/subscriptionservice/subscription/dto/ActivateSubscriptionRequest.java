package com.xtended.subscriptionservice.subscription.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xtended.subscriptionservice.subscription.model.SubscriptionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivateSubscriptionRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Subscription type is required")
    private SubscriptionType subscriptionType;

    @NotNull(message = "Activation date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate activationDate;
}
