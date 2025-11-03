package com.example.notifi.api.config;

import com.example.notifi.common.messaging.AmqpConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the API side of the AMQP topology so that publishing is possible without sharing databases.
 */
@Configuration
public class AmqpConfig {

    @Bean
    public DirectExchange notifiExchange(@Value("${notifi.amqp.exchange:" + AmqpConstants.DEFAULT_EXCHANGE + "}") String exchange) {
        return new DirectExchange(exchange, true, false); // durable exchange shared with worker
    }

    @Bean
    public Queue ingestQueue(
        @Value("${notifi.amqp.ingest-queue:" + AmqpConstants.INGEST_QUEUE + "}") String queueName) {
        return QueueBuilder.durable(queueName).build(); // API declares queue to ensure existence in dev setups
    }

    @Bean
    public Binding ingestBinding(
        DirectExchange notifiExchange,
        Queue ingestQueue,
        @Value("${notifi.amqp.ingest-routing-key:" + AmqpConstants.INGEST_ROUTING_KEY + "}") String routingKey) {
        return BindingBuilder.bind(ingestQueue).to(notifiExchange).with(routingKey); // route new notifications to worker ingestion
    }

    @Bean
    public Jackson2JsonMessageConverter apiMessageConverter() {
        return new Jackson2JsonMessageConverter(); // serialize NotificationTaskMessage as JSON
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter apiMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(apiMessageConverter); // ensure DTOs are serialized as JSON payloads
        return template;
    }
}
