ALTER TABLE client
    ADD COLUMN "keycloakClientId" TEXT;

CREATE UNIQUE INDEX uq_client_keycloak_client_id
    ON client ("keycloakClientId")
    WHERE "keycloakClientId" IS NOT NULL;

UPDATE client
SET "keycloakClientId" = 'demo-client',
    "updatedAt" = now()
WHERE id = '00000000-0000-0000-0000-000000000001';
