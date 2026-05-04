package com.xtended.subscriptionservice.subscription.configuration;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xtended.subscriptionservice.subscription.controller.api.SubscriptionController;
import com.xtended.subscriptionservice.subscription.controller.impl.SubscriptionControllerImpl;
import com.xtended.subscriptionservice.subscription.service.InvoiceService;
import com.xtended.subscriptionservice.subscription.service.SubscriptionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ApplicationConfiguration {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public SubscriptionController subscriptionController(
            InvoiceService invoiceService,
            SubscriptionService subscriptionService) {
        return new SubscriptionControllerImpl(invoiceService, subscriptionService);
    }
}
