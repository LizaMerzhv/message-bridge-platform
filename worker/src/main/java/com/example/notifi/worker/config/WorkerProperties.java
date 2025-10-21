package com.example.notifi.worker.config;

import com.example.notifi.worker.model.AmqpConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "notifi")
public class WorkerProperties {
  private Scheduler scheduler = new Scheduler();
  private Amqp amqp = new Amqp();
  private Consumer consumer = new Consumer();

  public Scheduler getScheduler() {
    return scheduler;
  }

  public Amqp getAmqp() {
    return amqp;
  }

  public Consumer getConsumer() {
    return consumer;
  }

  public static class Scheduler {
    private int batchSize = 50;
    private long scanIntervalMs = 2000L;

    public int getBatchSize() {
      return batchSize;
    }

    public void setBatchSize(int batchSize) {
      this.batchSize = batchSize;
    }

    public long getScanIntervalMs() {
      return scanIntervalMs;
    }

    public void setScanIntervalMs(long scanIntervalMs) {
      this.scanIntervalMs = scanIntervalMs;
    }
  }

  public static class Amqp {
    private String exchange = AmqpConstants.DEFAULT_EXCHANGE;
    private String dlx = AmqpConstants.DEFAULT_DLX;
    private String tasksQueue = "notify.tasks";
    private String retryQueue = "notify.retry";
    private String dlq = "notify.dlq";
    private String tasksRoutingKey = AmqpConstants.TASKS_ROUTING_KEY;
    private String retryRoutingKey = AmqpConstants.RETRY_ROUTING_KEY;
    private String dlqRoutingKey = AmqpConstants.DLQ_ROUTING_KEY;

    public String getExchange() {
      return exchange;
    }

    public void setExchange(String exchange) {
      this.exchange = exchange;
    }

    public String getDlx() {
      return dlx;
    }

    public void setDlx(String dlx) {
      this.dlx = dlx;
    }

    public String getTasksQueue() {
      return tasksQueue;
    }

    public void setTasksQueue(String tasksQueue) {
      this.tasksQueue = tasksQueue;
    }

    public String getRetryQueue() {
      return retryQueue;
    }

    public void setRetryQueue(String retryQueue) {
      this.retryQueue = retryQueue;
    }

    public String getDlq() {
      return dlq;
    }

    public void setDlq(String dlq) {
      this.dlq = dlq;
    }

    public String getTasksRoutingKey() {
      return tasksRoutingKey;
    }

    public void setTasksRoutingKey(String tasksRoutingKey) {
      this.tasksRoutingKey = tasksRoutingKey;
    }

    public String getRetryRoutingKey() {
      return retryRoutingKey;
    }

    public void setRetryRoutingKey(String retryRoutingKey) {
      this.retryRoutingKey = retryRoutingKey;
    }

    public String getDlqRoutingKey() {
      return dlqRoutingKey;
    }

    public void setDlqRoutingKey(String dlqRoutingKey) {
      this.dlqRoutingKey = dlqRoutingKey;
    }
  }

  public static class Consumer {
    private int concurrency = 2;
    private int prefetch = 20;

    @NestedConfigurationProperty private Retry retry = new Retry();

    public int getConcurrency() {
      return concurrency;
    }

    public void setConcurrency(int concurrency) {
      this.concurrency = concurrency;
    }

    public int getPrefetch() {
      return prefetch;
    }

    public void setPrefetch(int prefetch) {
      this.prefetch = prefetch;
    }

    public Retry getRetry() {
      return retry;
    }
  }

  public static class Retry {
    private int maxAttempts = 3;

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }
  }
}
