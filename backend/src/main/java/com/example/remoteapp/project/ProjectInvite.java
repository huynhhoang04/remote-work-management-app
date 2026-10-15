package com.example.remoteapp.project;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Link mời tham gia dự án (bảng {@code project_invites}) — FR-B03 rút gọn:
 * chỉ còn link/mã mời, không gửi email.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3
 * @reference docs/04-thiet-ke-backend.md §1.3 (điểm 3 — B03)
 */
@Entity
@Table(name = "project_invites")
@Getter
@Setter
public class ProjectInvite extends BaseEntity {

    /** Dự án được mời tham gia. */
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    /** Mã mời ngẫu nhiên 12 ký tự [A-Z0-9], unique. */
    @Column(name = "code", nullable = false, unique = true, length = 12)
    private String code;

    /** Người tạo link mời. */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    /** Hạn dùng của link (null = không giới hạn). */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** Đã bị thu hồi hay chưa. */
    @Column(name = "revoked", nullable = false)
    private boolean revoked;
}
