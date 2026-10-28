-- V3: task, nhan, checklist (subtask), binh luan
-- @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md muc C
-- @reference docs/04-thiet-ke-backend.md §5.2

CREATE TABLE tasks (
    id             UUID PRIMARY KEY,
    project_id     UUID          NOT NULL REFERENCES projects (id),
    parent_task_id UUID          REFERENCES tasks (id),
    title          VARCHAR(200)  NOT NULL,
    description    VARCHAR(5000),
    status         VARCHAR(20)   NOT NULL,
    priority       VARCHAR(20)   NOT NULL,
    start_date     TIMESTAMPTZ,
    deadline       TIMESTAMPTZ,
    position       DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_by     UUID          NOT NULL REFERENCES users (id),
    completed_at   TIMESTAMPTZ,
    version        BIGINT        NOT NULL DEFAULT 0,
    deleted_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE task_assignees (
    id         UUID PRIMARY KEY,
    task_id    UUID        NOT NULL REFERENCES tasks (id),
    user_id    UUID        NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_task_assignees UNIQUE (task_id, user_id)
);

CREATE TABLE labels (
    id         UUID PRIMARY KEY,
    project_id UUID        NOT NULL REFERENCES projects (id),
    name       VARCHAR(50) NOT NULL,
    color      VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_labels_project_name UNIQUE (project_id, name)
);

CREATE TABLE task_labels (
    id         UUID PRIMARY KEY,
    task_id    UUID        NOT NULL REFERENCES tasks (id),
    label_id   UUID        NOT NULL REFERENCES labels (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_task_labels UNIQUE (task_id, label_id)
);

CREATE TABLE task_comments (
    id         UUID PRIMARY KEY,
    task_id    UUID          NOT NULL REFERENCES tasks (id),
    user_id    UUID          NOT NULL REFERENCES users (id),
    content    VARCHAR(2000) NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_project ON tasks (project_id);
CREATE INDEX idx_tasks_deadline ON tasks (deadline);
CREATE INDEX idx_tasks_status ON tasks (status);
CREATE INDEX idx_tasks_parent ON tasks (parent_task_id);
CREATE INDEX idx_tasks_board ON tasks (project_id, status, position);
CREATE INDEX idx_task_assignees_user ON task_assignees (user_id);
CREATE INDEX idx_task_labels_label ON task_labels (label_id);
CREATE INDEX idx_task_comments_task ON task_comments (task_id, created_at);
