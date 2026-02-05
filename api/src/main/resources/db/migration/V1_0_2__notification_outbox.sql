CREATE TABLE outbox
(
    id              UUID PRIMARY KEY,
    "messageKey"    VARCHAR(128) NOT NULL,
    "eventType"     VARCHAR(64)  NOT NULL,
    payload         JSONB        NOT NULL,
    status          TEXT         NOT NULL CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    attempts        INTEGER      NOT NULL DEFAULT 0,
    "lastAttemptAt" TIMESTAMPTZ,
    "publishedAt"   TIMESTAMPTZ,
    "createdAt"     TIMESTAMPTZ  NOT NULL,
    "updatedAt"     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_outbox_message_key_len CHECK (char_length("messageKey") <= 128)
);

ALTER TABLE outbox
    ADD CONSTRAINT uq_outbox_message_key UNIQUE ("messageKey");

CREATE INDEX ix_outbox_status_created_at ON outbox (status, "createdAt");
