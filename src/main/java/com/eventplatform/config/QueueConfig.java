package com.eventplatform.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {
    // New names avoid silently changing existing QA/QD arguments on an installed broker.
    public static final String EXCHANGE = "event-trading.orders.v1";
    public static final String QUEUE = "event-trading.orders.v1";
    public static final String ROUTING_KEY = "create";
    @Bean DirectExchange orderExchange() { return new DirectExchange(EXCHANGE, true, false); }
    @Bean Queue orderQueue() { return QueueBuilder.durable(QUEUE)
        .deadLetterExchange(EXCHANGE + ".failed").deadLetterRoutingKey("failed").build(); }
    @Bean DirectExchange failedExchange() { return new DirectExchange(EXCHANGE + ".failed", true, false); }
    @Bean Queue failedQueue() { return QueueBuilder.durable(QUEUE + ".failed").maxLength(10000).build(); }
    @Bean Binding orderBinding() { return BindingBuilder.bind(orderQueue()).to(orderExchange()).with(ROUTING_KEY); }
    @Bean Binding failedBinding() { return BindingBuilder.bind(failedQueue()).to(failedExchange()).with("failed"); }
}
