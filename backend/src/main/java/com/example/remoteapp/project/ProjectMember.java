package com.example.remoteapp.project;

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
 * Thành viên dự án + vai trò (bảng {@code project_members}).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2
 * @reference docs/04-thiet-ke-backend.md §5.2
 */
@Entity
@Table(name = "project_members")
@Getter
@Setter
public class ProjectMember extends BaseEntity {

    /** Dự án tham gia. */
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    /** Người dùng tham gia. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Vai trò trong dự án. */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ProjectRole role;

    /** Thời điểm tham gia. */
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();
}
