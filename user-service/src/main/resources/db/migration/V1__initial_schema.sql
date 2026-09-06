CREATE TABLE roles
(
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(20) NOT NULL UNIQUE
);

INSERT INTO roles (name)
VALUES ('ADMIN'),
       ('USER');

CREATE TABLE users
(
    id      UUID PRIMARY KEY,
    email   VARCHAR(255) NOT NULL UNIQUE,
    role_id UUID         NOT NULL REFERENCES roles (id)
);

INSERT INTO users (id, email, role_id)
VALUES ('08a8684b-db88-4b73-90a9-3cd1661f5466', 'admin@example.com',
        (SELECT id FROM roles WHERE name = 'ADMIN')),
       ('1e542837-bfc9-4790-9195-8a5bc03dfa62', 'test1@example.com',
        (SELECT id FROM roles WHERE name = 'USER')),
       ('dc005267-ff5b-4806-bcc8-d08807e87214', 'test2@example.com',
        (SELECT id FROM roles WHERE name = 'USER')),
       ('9e4f18c0-ec73-4936-aeb7-9e420b05ea13', 'test3@example.com',
        (SELECT id FROM roles WHERE name = 'USER'));
