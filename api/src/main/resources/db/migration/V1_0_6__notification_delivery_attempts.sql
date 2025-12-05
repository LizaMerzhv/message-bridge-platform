CREATE TABLE notification_delivery_attempt (
                                               id UUID PRIMARY KEY,
                                               "notificationId" UUID NOT NULL REFERENCES notification (id),
                                               attempt INTEGER NOT NULL,
                                               status TEXT NOT NULL CHECK (status IN ('SENT', 'FAILED')),
                                               "errorCode" TEXT,
                                               "errorMessage" TEXT,
                                               "occurredAt" TIMESTAMPTZ NOT NULL,
                                               CONSTRAINT uq_notification_delivery_attempt UNIQUE ("notificationId", attempt)
);

CREATE INDEX ix_notification_delivery_attempt_notification ON notification_delivery_attempt ("notificationId");
CREATE INDEX ix_notification_delivery_attempt_status ON notification_delivery_attempt (status);
