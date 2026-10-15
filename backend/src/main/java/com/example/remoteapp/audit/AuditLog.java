package com.example.remoteapp.audit;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Nhật ký hoạt động (bảng {@code audit_logs}) — Owner dự án xem được.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §G
 * @reference docs/04-thiet-ke-backend.md §5.2, §12
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog extends BaseEntity {

    /** Người thực hiện hành động. */
    @Column(name = "actor_id")
    private UUID actorId;

    /** Dự án liên quan (null với hành động cấp tài khoản). */
    @Column(name = "project_id")
    private UUID projectId;

    /** Hành động được ghi. */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction action;

    /** Loại đối tượng bị tác động (PROJECT, MEMBER, TASK...). */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /** Id đối tượng bị tác động. */
    @Column(name = "entity_id")
    private UUID entityId;

    /** Chi tiết bổ sung (chuỗi ngắn, ví dụ "role=MEMBER"). */
    @Column(name = "metadata", length = 2000)
    private String metadata;
}
