CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE notification (
                              id UUID PRIMARY KEY,
                              "clientId" UUID NOT NULL,
                              "externalRequestId" VARCHAR(64) NOT NULL,
                              "sendAt" TIMESTAMPTZ,
                              status TEXT NOT NULL CHECK (status IN ('CREATED','QUEUED','SENT','FAILED')),
                              channel TEXT NOT NULL CHECK (channel = 'EMAIL'),
                              "to" VARCHAR(254) NOT NULL,
                              subject TEXT,
                              "templateCode" VARCHAR(64),
                              variables JSONB,
                              "traceId" TEXT,
                              "webhookUrl" TEXT,
                              "webhookSecret" TEXT,
                              attempts INTEGER NOT NULL DEFAULT 0,
                              "createdAt" TIMESTAMPTZ NOT NULL,
                              "updatedAt" TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_worker_notification_status_send_at ON notification (status, "sendAt");
CREATE INDEX ix_worker_notification_client ON notification ("clientId");

CREATE TABLE delivery (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          "notificationId" UUID NOT NULL REFERENCES notification(id) ON DELETE CASCADE,
                          status TEXT NOT NULL CHECK (status IN ('PENDING','SENT','FAILED')),
                          attempt INTEGER NOT NULL,
                          channel TEXT NOT NULL,
                          "to" VARCHAR(254) NOT NULL,
                          subject TEXT,
                          "errorCode" TEXT,
                          "errorMessage" TEXT,
                          "createdAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
                          "lastAttemptAt" TIMESTAMPTZ
);

CREATE INDEX ix_worker_delivery_notification_attempt ON delivery ("notificationId", attempt);
