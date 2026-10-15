package com.example.remoteapp.audit;

/**
 * Các hành động được ghi vào audit log (plan 03 §G).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §G
 * @reference docs/04-thiet-ke-backend.md §12
 */
public enum AuditAction {

    PROJECT_CREATED,
    PROJECT_UPDATED,
    PROJECT_DELETED,

    MEMBER_JOINED,
    MEMBER_REMOVED,
    ROLE_CHANGED,

    INVITE_CREATED,
    INVITE_REVOKED,

    TASK_CREATED,
    TASK_UPDATED,
    TASK_DELETED,
    TASK_ASSIGNED,
    TASK_STATUS_CHANGED,
    COMMENT_CREATED,

    USER_PASSWORD_CHANGED
}
