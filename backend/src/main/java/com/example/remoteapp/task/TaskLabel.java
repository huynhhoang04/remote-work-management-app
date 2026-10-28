package com.example.remoteapp.task;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Liên kết task ↔ nhãn (plan 03 §C1).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
 * @reference docs/04-thiet-ke-backend.md §5.2
 */
@Entity
@Table(name = "task_labels")
@Getter
@Setter
public class TaskLabel extends BaseEntity {

    /** Task. */
    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    /** Nhãn. */
    @Column(name = "label_id", nullable = false)
    private UUID labelId;
}
