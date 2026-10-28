package com.example.remoteapp.task;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Nhãn theo dự án (plan 03 §C1).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
 * @reference docs/04-thiet-ke-backend.md §5.2
 */
@Entity
@Table(name = "labels")
@Getter
@Setter
public class Label extends BaseEntity {

    /** Dự án sở hữu nhãn. */
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    /** Tên nhãn. */
    @Column(nullable = false, length = 50)
    private String name;

    /** Màu hiển thị (hex hoặc tên màu). */
    @Column(nullable = false, length = 20)
    private String color;
}
