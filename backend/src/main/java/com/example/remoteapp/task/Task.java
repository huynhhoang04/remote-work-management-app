package com.example.remoteapp.task;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Task trong dự án — hỗ trợ timeline ({@code startDate} + {@code deadline}),
 * Kanban ({@code status} + {@code position}) và checklist ({@code parentTaskId}).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
 * @reference docs/04-thiet-ke-backend.md §5.2
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
public class Task extends BaseEntity {

    /** Dự án chứa task. */
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    /** Task cha (checklist/subtask); null nếu là task gốc. */
    @Column(name = "parent_task_id")
    private UUID parentTaskId;

    /** Tiêu đề. */
    @Column(nullable = false, length = 200)
    private String title;

    /** Mô tả chi tiết. */
    @Column(length = 5000)
    private String description;

    /** Trạng thái Kanban. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    /** Độ ưu tiên. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    /** Ngày bắt đầu (timeline). */
    @Column(name = "start_date")
    private Instant startDate;

    /** Hạn hoàn thành. */
    @Column(name = "deadline")
    private Instant deadline;

    /** Thứ tự trong cột Kanban (kéo thả). */
    @Column(nullable = false)
    private double position;

    /** Người tạo task. */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    /** Thời điểm hoàn thành (status = DONE). */
    @Column(name = "completed_at")
    private Instant completedAt;

    /** Phiên bản cho optimistic locking. */
    @Version
    @Column(nullable = false)
    private long version;

    /** Soft delete. */
    @Column(name = "deleted_at")
    private Instant deletedAt;
}
