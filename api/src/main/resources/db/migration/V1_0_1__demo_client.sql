INSERT INTO client (id, name, "apiKey", "rateLimitPerMin", "createdAt", "updatedAt")
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Demo client',
    'demo-123',
    60,
    now(),
    now()
)
ON CONFLICT ("apiKey") DO NOTHING;
