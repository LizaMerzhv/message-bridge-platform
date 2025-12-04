ALTER TABLE outbox
ALTER COLUMN "payload" TYPE text
    USING "payload"::text;
