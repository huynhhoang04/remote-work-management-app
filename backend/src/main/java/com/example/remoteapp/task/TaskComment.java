package com.example.remoteapp.task;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Bình luận trong task (kênh trao đổi chính của nhóm — plan 03 §C3).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3
 * @reference docs/04-thiet-ke-backend.md §5.2
 */
@Entity
@Table(name = "task_comments")
@Getter
@Setter
public class TaskComment extends BaseEntity {

    /** Task chứa bình luận. */
    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    /** Tác giả. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Nội dung (1–2000 ký tự). */
    @Column(nullable = false, length = 2000)
    private String content;

    /** Soft delete — hiển thị "đã xóa". */
    @Column(name = "deleted_at")
    private Instant deletedAt;
}
