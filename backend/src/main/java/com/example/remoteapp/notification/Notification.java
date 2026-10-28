package com.example.remoteapp.notification;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Thông báo trong trung tâm thông báo (nguồn sự thật; push chỉ là kênh đẩy).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E2
 * @reference docs/04-thiet-ke-backend.md §5.2
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification extends BaseEntity {

    /** Người nhận. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Loại thông báo. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    /** Tiêu đề hiển thị. */
    @Column(nullable = false, length = 200)
    private String title;

    /** Nội dung hiển thị. */
    @Column(nullable = false, length = 1000)
    private String body;

    /** Loại đối tượng tham chiếu (TASK, CONVERSATION...). */
    @Column(name = "ref_type", length = 30)
    private String refType;

    /** Id đối tượng tham chiếu (deep-link). */
    @Column(name = "ref_id")
    private UUID refId;

    /**
     * Khoá chống trùng cho nhắc deadline (UNIQUE, nullable).
     * Ví dụ: {@code REMIND_1D:<taskId>:<userId>} hoặc {@code OVERDUE:<taskId>:<userId>:2026-10-28}.
     */
    @Column(name = "dedupe_key", length = 200)
    private String dedupeKey;

    /** Thời điểm đọc (null = chưa đọc). */
    @Column(name = "read_at")
    private Instant readAt;
}
