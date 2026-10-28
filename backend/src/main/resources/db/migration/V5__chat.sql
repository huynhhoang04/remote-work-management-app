-- V5: chat (hoi thoai, thanh vien, tin nhan, read receipt)
-- @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F
-- @reference docs/04-thiet-ke-backend.md §5.2

CREATE TABLE conversations (
    id         UUID PRIMARY KEY,
    project_id UUID        REFERENCES projects (id),
    type       VARCHAR(10) NOT NULL,
    direct_key VARCHAR(80) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE conversation_members (
    id                   UUID PRIMARY KEY,
    conversation_id      UUID        NOT NULL REFERENCES conversations (id),
    user_id              UUID        NOT NULL REFERENCES users (id),
    last_read_message_id UUID,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_conversation_members UNIQUE (conversation_id, user_id)
);

CREATE TABLE messages (
    id                UUID PRIMARY KEY,
    conversation_id   UUID          NOT NULL REFERENCES conversations (id),
    sender_id         UUID          NOT NULL REFERENCES users (id),
    content           VARCHAR(4000) NOT NULL,
    reply_to_id       UUID,
    client_message_id UUID,
    deleted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_messages_client_id UNIQUE (conversation_id, client_message_id)
);

-- Mỗi dự án chỉ có 1 kênh GROUP (tạo lazy khi truy cập lần đầu)
CREATE UNIQUE INDEX uq_conversations_project_group
    ON conversations (project_id) WHERE type = 'GROUP';
CREATE INDEX idx_conversations_project ON conversations (project_id);
CREATE INDEX idx_conversation_members_user ON conversation_members (user_id);
CREATE INDEX idx_messages_conversation_time ON messages (conversation_id, created_at);
