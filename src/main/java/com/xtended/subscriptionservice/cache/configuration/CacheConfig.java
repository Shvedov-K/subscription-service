package com.xtended.subscriptionservice.cache.configuration;

import com.xtended.subscriptionservice.cache.controller.api.CacheController;
import com.xtended.subscriptionservice.cache.controller.impl.CacheControllerImpl;
import com.xtended.subscriptionservice.cache.service.CacheUserService;
import com.xtended.subscriptionservice.cache.service.SubscriptionServiceFallbackClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {
    @Bean
    public CacheController cacheController(CacheUserService cacheUserService,
                                           SubscriptionServiceFallbackClient fallbackClient) {
        return new CacheControllerImpl(cacheUserService, fallbackClient);
    }
}
