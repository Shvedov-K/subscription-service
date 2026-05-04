package com.xtended.subscriptionservice.cache.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Конфигурация для fallback-клиента
 */
@Configuration
public class CacheFallbackConfig {
    @Value("${app.subscription-service.url}")
    private String subscriptionServiceUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public String subscriptionServiceBaseUrl() {
        return subscriptionServiceUrl;
    }
}
