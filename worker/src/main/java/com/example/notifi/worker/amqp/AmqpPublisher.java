package com.example.notifi.worker.amqp;

import com.example.notifi.worker.model.NotificationMessage;
import com.example.notifi.worker.config.WorkerProperties;
import java.time.Duration;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class AmqpPublisher {
  private final RabbitTemplate rabbitTemplate;
  private final WorkerProperties properties;

  public AmqpPublisher(RabbitTemplate rabbitTemplate, WorkerProperties properties) {
    this.rabbitTemplate = rabbitTemplate;
    this.properties = properties;
  }

  public void publishTask(NotificationMessage message) {
    rabbitTemplate.convertAndSend(properties.getAmqp().getExchange(), properties.getAmqp().getTasksRoutingKey(), message);
  }

  public void publishRetry(NotificationMessage message, Duration ttl, Duration jitter) {
    long ttlMillis = Math.max(1, ttl.plus(jitter).toMillis());
    Message original = rabbitTemplate.getMessageConverter().toMessage(message, null);
    Message amqpMessage =
        MessageBuilder.withBody(original.getBody())
            .andProperties(original.getMessageProperties())
            .setExpiration(String.valueOf(ttlMillis))
            .build();
    rabbitTemplate.send(properties.getAmqp().getExchange(), properties.getAmqp().getRetryRoutingKey(), amqpMessage);
  }

  public void publishDlq(NotificationMessage message) {
    rabbitTemplate.convertAndSend(properties.getAmqp().getDlx(), properties.getAmqp().getDlqRoutingKey(), message);
  }
}
