package com.example.notifi.common.messaging;

public final class AmqpConstants {
    public static final String DEFAULT_EXCHANGE = "notifi.exchange";
    public static final String DEFAULT_DLX = "notifi.dlx";
    public static final String INGEST_QUEUE = "notify.ingest";
    public static final String TASKS_QUEUE = "notify.tasks";
    public static final String RETRY_QUEUE = "notify.retry";
    public static final String DLQ_QUEUE = "notify.dlq";
    public static final String INGEST_ROUTING_KEY = "ingest";
    public static final String TASKS_ROUTING_KEY = "tasks";
    public static final String RETRY_ROUTING_KEY = "retry";
    public static final String DLQ_ROUTING_KEY = "dlq";
    private AmqpConstants() {}
}
