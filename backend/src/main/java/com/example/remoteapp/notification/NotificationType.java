package com.example.remoteapp.notification;

/**
 * Loại thông báo trong trung tâm thông báo (plan 03 §E2).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E2
 */
public enum NotificationType {

    TASK_ASSIGNED,
    MENTION,
    REMIND_1D,
    REMIND_1H,
    OVERDUE,
    MESSAGE
}
