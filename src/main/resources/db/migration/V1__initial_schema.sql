CREATE TABLE users
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    username   VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE documents
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE document_retrievals
(
    id           UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    document_id  UUID        NOT NULL REFERENCES documents (id),
    retrieved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expired_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_documents_user_id ON documents (user_id);