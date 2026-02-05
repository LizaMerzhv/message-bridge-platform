package com.example.notifi.worker.config;

import com.example.notifi.worker.amqp.RetryPolicy;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(WorkerProperties.class)
public class WorkerConfiguration {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public RetryPolicy retryPolicy(WorkerProperties properties) {
    return new RetryPolicy(
        ThreadLocalRandom.current()::nextDouble, properties.getRetry().getMaxAttempts());
  }

  @Bean
  public DirectExchange notifiExchange(WorkerProperties properties) {
    return new DirectExchange(properties.getAmqp().getExchange(), true, false);
  }

  @Bean
  public DirectExchange deadLetterExchange(WorkerProperties properties) {
    return new DirectExchange(properties.getAmqp().getDlx(), true, false);
  }

  @Bean
  public Queue tasksQueue(WorkerProperties properties) {
    return QueueBuilder.durable(properties.getAmqp().getTasksQueue())
        .withArgument("x-dead-letter-exchange", properties.getAmqp().getDlx())
        .withArgument("x-dead-letter-routing-key", properties.getAmqp().getDlqRoutingKey())
        .build();
  }

  @Bean
  public Queue ingestQueue(WorkerProperties properties) {
    return QueueBuilder.durable(properties.getAmqp().getIngestQueue()).build();
  }

  @Bean
  public Queue retryQueue(WorkerProperties properties) {
    return QueueBuilder.durable(properties.getAmqp().getRetryQueue())
        .withArgument("x-dead-letter-exchange", properties.getAmqp().getExchange())
        .withArgument("x-dead-letter-routing-key", properties.getAmqp().getTasksRoutingKey())
        .build();
  }

  @Bean
  public Queue deadLetterQueue(WorkerProperties properties) {
    return QueueBuilder.durable(properties.getAmqp().getDlq()).build();
  }

  @Bean
  public Binding tasksBinding(
      WorkerProperties properties, Queue tasksQueue, DirectExchange notifiExchange) {
    return BindingBuilder.bind(tasksQueue)
        .to(notifiExchange)
        .with(properties.getAmqp().getTasksRoutingKey());
  }

  @Bean
  public Binding ingestBinding(
      WorkerProperties properties, Queue ingestQueue, DirectExchange notifiExchange) {
    return BindingBuilder.bind(ingestQueue)
        .to(notifiExchange)
        .with(properties.getAmqp().getIngestRoutingKey()); // route API events to ingestion queue
  }

  @Bean
  public Binding retryBinding(
      WorkerProperties properties, Queue retryQueue, DirectExchange notifiExchange) {
    return BindingBuilder.bind(retryQueue)
        .to(notifiExchange)
        .with(properties.getAmqp().getRetryRoutingKey());
  }

  @Bean
  public Binding dlqBinding(
      WorkerProperties properties, Queue deadLetterQueue, DirectExchange deadLetterExchange) {
    return BindingBuilder.bind(deadLetterQueue)
        .to(deadLetterExchange)
        .with(properties.getAmqp().getDlqRoutingKey());
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter);
    return template;
  }

  @Bean
  public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public SimpleRabbitListenerContainerFactory taskListenerContainerFactory(
      ConnectionFactory connectionFactory,
      WorkerProperties properties,
      Jackson2JsonMessageConverter messageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setConcurrentConsumers(properties.getConsumer().getConcurrency());
    factory.setPrefetchCount(properties.getConsumer().getPrefetch());
    factory.setMessageConverter(messageConverter);
    return factory;
  }

  @Bean
  public RestClient.Builder restClientBuilder() {
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(7));
    return RestClient.builder().requestFactory(requestFactory);
  }
}
