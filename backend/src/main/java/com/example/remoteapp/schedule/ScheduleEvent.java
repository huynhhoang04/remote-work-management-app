package com.example.remoteapp.schedule;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Sự kiện trong lịch làm việc cá nhân (FR-B01).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1
 * @reference docs/04-thiet-ke-backend.md §5.2
 */
@Entity
@Table(name = "schedule_events")
@Getter
@Setter
public class ScheduleEvent extends BaseEntity {

    /** Chủ sự kiện. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Tiêu đề. */
    @Column(nullable = false, length = 200)
    private String title;

    /** Mô tả. */
    @Column(length = 2000)
    private String description;

    /** Bắt đầu. */
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    /** Kết thúc. */
    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    /** Task liên kết (hiển thị deadline), có thể null. */
    @Column(name = "task_id")
    private UUID taskId;
}
