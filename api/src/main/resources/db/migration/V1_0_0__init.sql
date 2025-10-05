
CREATE TABLE client (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    "apiKey" TEXT NOT NULL UNIQUE,
    "webhookUrl" TEXT,
    "webhookSecret" TEXT,
    "rateLimitPerMin" INTEGER NOT NULL DEFAULT 60,
    "createdAt" TIMESTAMPTZ NOT NULL,
    "updatedAt" TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_client_apikey_len CHECK (char_length("apiKey") <= 64)
);


CREATE TABLE template (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    subject TEXT NOT NULL,
    "bodyHtml" TEXT,
    "bodyText" TEXT,
    status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    "createdAt" TIMESTAMPTZ NOT NULL,
    "updatedAt" TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_template_html_size CHECK ("bodyHtml" IS NULL OR octet_length("bodyHtml") <= 262144),
    CONSTRAINT ck_template_text_size CHECK ("bodyText" IS NULL OR octet_length("bodyText") <= 262144)
);


CREATE TABLE notification (
    id UUID PRIMARY KEY,
    "clientId" UUID NOT NULL REFERENCES client (id),
    "externalRequestId" VARCHAR(64) NOT NULL,
    channel TEXT NOT NULL CHECK (channel = 'email'),
    "to" VARCHAR(254) NOT NULL,
    subject TEXT,
    "templateCode" VARCHAR(64),
    variables JSONB,
    "sendAt" TIMESTAMPTZ,
    status TEXT NOT NULL CHECK (status IN ('CREATED', 'QUEUED', 'SENT', 'FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0,
    "createdAt" TIMESTAMPTZ NOT NULL,
    "updatedAt" TIMESTAMPTZ NOT NULL,

    CONSTRAINT notification_subject_template_xor
        CHECK ((subject IS NOT NULL) <> ("templateCode" IS NOT NULL)),

    CONSTRAINT ck_notification_subject_len
        CHECK (subject IS NULL OR char_length(subject) <= 200),
    CONSTRAINT fk_notification_template_code FOREIGN KEY ("templateCode") REFERENCES template (code)
);

ALTER TABLE notification
    ADD CONSTRAINT uq_notification_client_external UNIQUE ("clientId", "externalRequestId");

CREATE INDEX ix_notification_status_send_at ON notification (status, "sendAt");
CREATE INDEX ix_notification_client_created_at ON notification ("clientId", "createdAt");


CREATE TABLE delivery (
    id UUID PRIMARY KEY,
    "notificationId" UUID NOT NULL REFERENCES notification (id),
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempt INTEGER NOT NULL,
    channel TEXT,
    "to" VARCHAR(254),
    subject TEXT,
    "errorCode" TEXT,
    "errorMessage" TEXT,
    "createdAt" TIMESTAMPTZ NOT NULL,
    "lastAttemptAt" TIMESTAMPTZ
);

CREATE INDEX ix_delivery_notification_attempt ON delivery ("notificationId", attempt);
CREATE INDEX ix_delivery_status ON delivery (status);

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
