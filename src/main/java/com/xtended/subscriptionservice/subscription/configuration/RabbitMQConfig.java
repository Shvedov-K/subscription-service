package com.xtended.subscriptionservice.subscription.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "subscription.exchange";

    @Bean
    public TopicExchange subscriptionExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue subscriptionActivatedQueue() {
        return new Queue("subscription.activated.queue", true);
    }

    @Bean
    public Queue subscriptionDeactivatedQueue() {
        return new Queue("subscription.deactivated.queue", true);
    }

    @Bean
    public Queue externalInvoiceQueue() {
        return new Queue("external.invoice.queue", true);
    }

    @Bean
    public Queue invoiceIssuedQueue() {
        return new Queue("invoice.issued.queue", true);
    }

    @Bean
    public Binding bindActivated(Queue subscriptionActivatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(subscriptionActivatedQueue).to(exchange).with("subscription.activated");
    }

    @Bean
    public Binding bindDeactivated(Queue subscriptionDeactivatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(subscriptionDeactivatedQueue).to(exchange).with("subscription.deactivated");
    }

    @Bean
    public Binding bindExternalInvoice(Queue externalInvoiceQueue, TopicExchange exchange) {
        return BindingBuilder.bind(externalInvoiceQueue).to(exchange).with("invoice.issued");
    }

    @Bean
    public Binding bindInvoice(Queue invoiceIssuedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(invoiceIssuedQueue).to(exchange).with("invoice.issued");
    }
}
