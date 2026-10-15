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
 * Dự án (bảng {@code projects}) — cấp cao nhất của mô hình nhóm nhỏ
 * (không có workspace/tổ chức — plan 01 §1).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B1
 * @reference docs/04-thiet-ke-backend.md §5.2
 */
@Entity
@Table(name = "projects")
@Getter
@Setter
public class Project extends BaseEntity {

    /** Tên dự án (bắt buộc). */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Mô tả dự án (tuỳ chọn). */
    @Column(name = "description", length = 1000)
    private String description;

    /** Chủ dự án (OWNER). */
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    /** Thời điểm soft delete (null = đang hoạt động). */
    @Column(name = "deleted_at")
    private Instant deletedAt;
}
