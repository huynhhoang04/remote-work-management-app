package com.example.remoteapp.common;

import lombok.Getter;

/**
 * Danh mục mã lỗi nghiệp vụ — trả về thống nhất qua {@link ErrorResponse}
 * (plan 02 §1.1, §13).
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §13
 */
@Getter
public enum ErrorCode {

    AUTH_INVALID_CREDENTIALS(401),
    AUTH_TOKEN_EXPIRED(401),
    AUTH_TOKEN_INVALID(401),
    AUTH_REFRESH_INVALID(401),
    AUTH_UNAUTHORIZED(401),
    AUTH_RATE_LIMITED(429),

    USER_EMAIL_EXISTS(409),
    USER_NOT_FOUND(404),
    USER_LOCKED(403),
    RESET_TOKEN_INVALID(400),
    PASSWORD_INVALID(400),

    PROJECT_NOT_FOUND(404),
    PROJECT_FORBIDDEN(403),
    INVITE_INVALID(400),
    MEMBER_NOT_FOUND(404),
    MEMBER_ROLE_INVALID(400),

    TASK_NOT_FOUND(404),
    TASK_DEADLINE_INVALID(400),
    TASK_PARENT_INVALID(400),
    TASK_ASSIGNEE_INVALID(400),
    TASK_VERSION_CONFLICT(409),
    LABEL_NOT_FOUND(404),
    LABEL_DUPLICATE(409),
    COMMENT_NOT_FOUND(404),
    COMMENT_FORBIDDEN(403),

    SCHEDULE_EVENT_NOT_FOUND(404),
    SCHEDULE_TIME_INVALID(400),
    STATS_RANGE_INVALID(400),

    FILE_NOT_FOUND(404),
    FILE_TOO_LARGE(400),
    FILE_TYPE_NOT_ALLOWED(400),
    STORAGE_ERROR(500),

    CONVERSATION_NOT_FOUND(404),
    CONVERSATION_FORBIDDEN(403),
    MESSAGE_NOT_FOUND(404),
    NOTIFICATION_NOT_FOUND(404),

    VALIDATION_ERROR(400),
    INTERNAL_ERROR(500);

    /** HTTP status tương ứng sẽ trả về client. */
    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }
}
