CREATE TABLE client
(
    id                UUID PRIMARY KEY,
    name              TEXT        NOT NULL,
    "apiKey"          TEXT        NOT NULL UNIQUE,
    "rateLimitPerMin" INTEGER     NOT NULL DEFAULT 60,
    "createdAt"       TIMESTAMPTZ NOT NULL,
    "updatedAt"       TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_client_apikey_len CHECK (char_length("apiKey") <= 64)
);
