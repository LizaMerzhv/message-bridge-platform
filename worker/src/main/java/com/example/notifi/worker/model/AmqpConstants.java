package com.example.notifi.worker.model;

/**
 * Shared AMQP topology defaults for the notifi services.
 */
public final class AmqpConstants {
    public static final String DEFAULT_EXCHANGE = "notifi.exchange";
    public static final String DEFAULT_DLX = "notifi.dlx";

    public static final String TASKS_QUEUE = "notify.tasks";
    public static final String RETRY_QUEUE = "notify.retry";
    public static final String DLQ_QUEUE = "notify.dlq";

    public static final String TASKS_ROUTING_KEY = "tasks";
    public static final String RETRY_ROUTING_KEY = "retry";
    public static final String DLQ_ROUTING_KEY = "dlq";

    private AmqpConstants() {}
}
