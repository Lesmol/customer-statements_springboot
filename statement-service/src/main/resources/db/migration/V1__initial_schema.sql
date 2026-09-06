CREATE TABLE documents
(
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL,
    filename    VARCHAR(255) NOT NULL,
    file_hash   VARCHAR(64)  NOT NULL,
    uploaded_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE document_retrievals
(
    id           UUID PRIMARY KEY,
    document_id  UUID        NOT NULL REFERENCES documents (id),
    retrieved_at TIMESTAMPTZ NOT NULL,
    expired_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_documents_user_id ON documents (user_id);
CREATE INDEX idx_file_hash ON documents (file_hash);
