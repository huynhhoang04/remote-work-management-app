-- V2: projects + thanh vien + link moi + audit log
-- @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md muc B, G
-- @reference docs/04-thiet-ke-backend.md §5.2

CREATE TABLE projects (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(1000),
    owner_id    UUID          NOT NULL REFERENCES users (id),
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE project_members (
    id         UUID PRIMARY KEY,
    project_id UUID        NOT NULL REFERENCES projects (id),
    user_id    UUID        NOT NULL REFERENCES users (id),
    role       VARCHAR(20) NOT NULL,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_project_members UNIQUE (project_id, user_id)
);

CREATE TABLE project_invites (
    id         UUID PRIMARY KEY,
    project_id UUID        NOT NULL REFERENCES projects (id),
    code       VARCHAR(12) NOT NULL UNIQUE,
    created_by UUID        NOT NULL REFERENCES users (id),
    expires_at TIMESTAMPTZ,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY,
    actor_id    UUID         REFERENCES users (id),
    project_id  UUID         REFERENCES projects (id),
    action      VARCHAR(50)  NOT NULL,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   UUID,
    metadata    VARCHAR(2000),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_project_members_user ON project_members (user_id);
CREATE INDEX idx_project_invites_project ON project_invites (project_id);
CREATE INDEX idx_audit_logs_project ON audit_logs (project_id, created_at);
