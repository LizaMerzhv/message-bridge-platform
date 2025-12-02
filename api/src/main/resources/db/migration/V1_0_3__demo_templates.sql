INSERT INTO template (id, code, subject, "bodyHtml", "bodyText", status, "createdAt", "updatedAt")
VALUES
    ('b8a4627a-6f01-4c4d-9f6f-7e7ec6af8d01', 'WELCOME_TEMPLATE', 'Welcome, #{username}!',
     '<p>Hello <strong>#{username}</strong>, welcome to Notifi!</p><p>We are glad you are here.</p>',
     'Hello #{username}, welcome to Notifi! We are glad you are here.',
     'ACTIVE', now(), now()),
    ('1c6a0272-1de7-4f8b-8e80-0ecf8d1c5b30', 'NEWS_WELCOME_1', 'Your daily news digest',
     '<p>Hi #{username}, here is your quick news digest.</p><ul><li>Top story: #{headline}</li></ul>',
     'Hi #{username}, here is your quick news digest. Top story: #{headline}.',
     'ACTIVE', now(), now()),
    ('d1c68453-5120-459b-b68a-5c4e99e6b4b7', 'PASSWORD_RESET', 'Reset your password',
     '<p>Hi #{username}, click the link to reset your password: #{resetLink}</p>',
     'Hi #{username}, use this link to reset your password: #{resetLink}',
     'ACTIVE', now(), now()),
    ('a3a1b9c2-905c-4f6c-86b0-3d3ef2a0ea18', 'PROMO_DISCOUNT', 'Exclusive discount for #{username}',
     '<p>Hey #{username}, enjoy an exclusive <strong>#{discount}%</strong> discount just for you!</p>',
     'Hey #{username}, enjoy an exclusive #{discount}% discount just for you!',
     'ACTIVE', now(), now())
    ON CONFLICT (code) DO NOTHING;
