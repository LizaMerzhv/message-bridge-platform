package com.example.notifi.worker.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.notifi.worker.amqp.AmqpPublisher;
import com.example.notifi.worker.metrics.WorkerMetrics;
import com.example.notifi.worker.scheduler.SchedulerService;
import com.example.notifi.worker.webhook.HmacSigner;
import com.example.notifi.worker.webhook.HttpWebhookDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootTest(
    classes = WorkerConfigurationTest.TestApplication.class,
    properties = {
      "notifi.scheduler.batch-size=7",
      "notifi.amqp.exchange=test-exchange",
      "notifi.consumer.prefetch=15"
    })
class WorkerConfigurationTest {

  @MockBean
  private com.example.notifi.worker.data.repository.NotificationRepository notificationRepository;

  @MockBean private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

  @Test
  void contextProvidesBeansAndBindsProperties(
      WorkerProperties properties,
      SchedulerService schedulerService,
      AmqpPublisher publisher,
      HttpWebhookDispatcher dispatcher,
      WorkerMetrics metrics) {
    assertThat(properties.getScheduler().getBatchSize()).isEqualTo(7);
    assertThat(properties.getAmqp().getExchange()).isEqualTo("test-exchange");
    assertThat(properties.getConsumer().getPrefetch()).isEqualTo(15);

    assertThat(schedulerService).isNotNull();
    assertThat(publisher).isNotNull();
    assertThat(dispatcher).isNotNull();
    assertThat(metrics).isNotNull();
  }

  @SpringBootApplication
  @Import({
    WorkerConfiguration.class,
    SchedulerService.class,
    AmqpPublisher.class,
    HttpWebhookDispatcher.class,
    WorkerMetrics.class,
    HmacSigner.class,
    com.example.notifi.worker.data.entity.NotificationMessageMapper.class
  })
  static class TestApplication {
    @Bean
    ConnectionFactory connectionFactory() {
      return new CachingConnectionFactory("localhost");
    }
  }
}
