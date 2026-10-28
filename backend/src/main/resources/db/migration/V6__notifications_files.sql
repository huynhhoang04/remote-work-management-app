-- V6: trung tam thong bao + file dinh kem
-- @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E, §H
-- @reference docs/04-thiet-ke-backend.md §5.2

CREATE TABLE notifications (
    id         UUID PRIMARY KEY,
    user_id    UUID          NOT NULL REFERENCES users (id),
    type       VARCHAR(30)   NOT NULL,
    title      VARCHAR(200)  NOT NULL,
    body       VARCHAR(1000) NOT NULL,
    ref_type   VARCHAR(30),
    ref_id     UUID,
    dedupe_key VARCHAR(200),
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Chong trung nhac deadline: (task, user, moc) chi 1 lan; NULL duoc phep nhieu
CREATE UNIQUE INDEX uq_notifications_dedupe
    ON notifications (dedupe_key) WHERE dedupe_key IS NOT NULL;
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, read_at);
CREATE INDEX idx_notifications_user_time ON notifications (user_id, created_at);

CREATE TABLE files (
    id          UUID PRIMARY KEY,
    owner_id    UUID         NOT NULL REFERENCES users (id),
    project_id  UUID         REFERENCES projects (id),
    task_id     UUID         REFERENCES tasks (id),
    message_id  UUID         REFERENCES messages (id),
    name        VARCHAR(255) NOT NULL,
    mime_type   VARCHAR(100) NOT NULL,
    size        BIGINT       NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_files_task ON files (task_id);
CREATE INDEX idx_files_message ON files (message_id);
CREATE INDEX idx_files_owner ON files (owner_id);
