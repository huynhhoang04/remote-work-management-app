package com.example.remoteapp.task;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Quan hệ gán task cho thành viên dự án (plan 03 §C2).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C2
 * @reference docs/04-thiet-ke-backend.md §5.2
 */
@Entity
@Table(name = "task_assignees")
@Getter
@Setter
public class TaskAssignee extends BaseEntity {

    /** Task được gán. */
    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    /** Người được gán. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
