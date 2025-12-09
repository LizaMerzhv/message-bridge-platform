package com.example.notifi.worker.amqp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.notifi.common.messaging.NotificationTaskMessage;
import com.example.notifi.common.model.Channel;
import com.example.notifi.worker.config.WorkerProperties;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;

@ExtendWith(MockitoExtension.class)
class AmqpPublisherTest {

  @Mock private RabbitTemplate rabbitTemplate;

  private WorkerProperties properties;
  private AmqpPublisher publisher;
  private NotificationTaskMessage message;

  @BeforeEach
  void setUp() {
    properties = new WorkerProperties();
    message =
        new NotificationTaskMessage(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ext-1",
            Channel.EMAIL,
            "user@example.com",
            "Subject",
            "WELCOME",
            null,
            java.time.Instant.now(),
            java.time.Instant.now(),
            1,
            "trace",
            "http://webhook",
            "secret");
    publisher = new AmqpPublisher(rabbitTemplate, properties);
  }

  @Test
  void publishTask_SendsToConfiguredExchange() {
    publisher.publishTask(message);

    verify(rabbitTemplate)
        .convertAndSend(
            properties.getAmqp().getExchange(), properties.getAmqp().getTasksRoutingKey(), message);
  }

  @Test
  void publishRetry_SetsExpirationAndRoutingKey() {
    MessageConverter converter = new SimpleMessageConverter();
    when(rabbitTemplate.getMessageConverter()).thenReturn(converter);

    publisher.publishRetry(message, Duration.ofSeconds(5), Duration.ZERO);

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(rabbitTemplate)
        .send(
            org.mockito.Mockito.eq(properties.getAmqp().getExchange()),
            org.mockito.Mockito.eq(properties.getAmqp().getRetryRoutingKey()),
            captor.capture());

    assertThat(captor.getValue().getMessageProperties().getExpiration()).isEqualTo("5000");
  }

  @Test
  void publishDlq_SendsToDeadLetterExchange() {
    publisher.publishDlq(message);

    verify(rabbitTemplate)
        .convertAndSend(
            properties.getAmqp().getDlx(), properties.getAmqp().getDlqRoutingKey(), message);
  }
}
