package com.example.notifi.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notifi")
public class WorkerProperties {

  private Scheduler scheduler = new Scheduler();
  private Amqp amqp = new Amqp();
  private Consumer consumer = new Consumer();
  private Retry retry = new Retry();

  public Scheduler getScheduler() {
    return scheduler;
  }

  public void setScheduler(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  public Amqp getAmqp() {
    return amqp;
  }

  public void setAmqp(Amqp amqp) {
    this.amqp = amqp;
  }

  public Consumer getConsumer() {
    return consumer;
  }

  public void setConsumer(Consumer consumer) {
    this.consumer = consumer;
  }

  public Retry getRetry() {
    return retry;
  }

  public void setRetry(Retry retry) {
    this.retry = retry;
  }

  // --- nested types ---

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
    private String exchange = "notifi.exchange";
    private String dlx = "notifi.dlx";
    private String ingestQueue = "notify.ingest";
    private String tasksQueue = "notify.tasks";
    private String retryQueue = "notify.retry";
    private String dlq = "notify.dlq";

    private String ingestRoutingKey = "ingest";
    private String tasksRoutingKey = "tasks";
    private String retryRoutingKey = "retry";
    private String dlqRoutingKey = "dlq";

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

    public String getIngestQueue() {
      return ingestQueue;
    }

    public void setIngestQueue(String ingestQueue) {
      this.ingestQueue = ingestQueue;
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

    public String getIngestRoutingKey() {
      return ingestRoutingKey;
    }

    public void setIngestRoutingKey(String ingestRoutingKey) {
      this.ingestRoutingKey = ingestRoutingKey;
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
