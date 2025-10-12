package com.example.notifi.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notifi.amqp")
public class AmqpProperties {

    /** Exchange name used for notification events. */
    private String exchange = "notifi.exchange";

    private Routing routing = new Routing();

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public Routing getRouting() {
        return routing;
    }

    public void setRouting(Routing routing) {
        this.routing = routing;
    }

    public static class Routing {
        private String tasks = "notify.tasks";
        private String retry = "notify.retry";
        private String dlq = "notify.dlq";

        public String getTasks() {
            return tasks;
        }

        public void setTasks(String tasks) {
            this.tasks = tasks;
        }

        public String getRetry() {
            return retry;
        }

        public void setRetry(String retry) {
            this.retry = retry;
        }

        public String getDlq() {
            return dlq;
        }

        public void setDlq(String dlq) {
            this.dlq = dlq;
        }
    }
}
