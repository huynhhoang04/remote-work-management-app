-- V4: lich lam viec ca nhan
-- @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1
-- @reference docs/04-thiet-ke-backend.md §5.2

CREATE TABLE schedule_events (
    id          UUID PRIMARY KEY,
    user_id     UUID          NOT NULL REFERENCES users (id),
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    start_time  TIMESTAMPTZ   NOT NULL,
    end_time    TIMESTAMPTZ   NOT NULL,
    task_id     UUID          REFERENCES tasks (id),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_schedule_events_user_time ON schedule_events (user_id, start_time);
