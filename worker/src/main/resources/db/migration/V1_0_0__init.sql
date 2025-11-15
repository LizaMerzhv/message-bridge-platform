CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE notification (
                              id UUID PRIMARY KEY,
                              client_id UUID NOT NULL,
                              external_request_id VARCHAR(64) NOT NULL,
                              send_at TIMESTAMPTZ,
                              status TEXT NOT NULL CHECK (status IN ('CREATED','QUEUED','SENT','FAILED')),
                              channel TEXT NOT NULL CHECK (channel = 'EMAIL'),
                              recipient VARCHAR(254) NOT NULL,
                              subject TEXT,
                              template_code VARCHAR(64),
                              variables JSONB,
                              trace_id TEXT,
                              webhook_url TEXT,
                              webhook_secret TEXT,
                              attempts INTEGER NOT NULL DEFAULT 0,
                              created_at TIMESTAMPTZ NOT NULL,
                              updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_worker_notification_status_send_at ON notification (status, send_at);
CREATE INDEX ix_worker_notification_client ON notification (client_id);

CREATE TABLE delivery (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          notification_id UUID NOT NULL REFERENCES notification(id) ON DELETE CASCADE,
                          status TEXT NOT NULL CHECK (status IN ('PENDING','SENT','FAILED')),
                          attempt INTEGER NOT NULL,
                          channel TEXT NOT NULL,
                          recipient VARCHAR(254) NOT NULL,
                          subject TEXT,
                          error_code TEXT,
                          error_message TEXT,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                          last_attempt_at TIMESTAMPTZ
);

CREATE INDEX ix_worker_delivery_notification_attempt ON delivery (notification_id, attempt);
