package com.example.remoteapp.common;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Lớp nền chung cho mọi entity: khoá chính UUID + timestamps + soft delete (plan 01 §8).
 *
 * <p>UUID khoá chính để client (mobile) có thể tự sinh ID nếu cần (ADR-06);
 * {@code createdAt}/{@updatedAt} tự quản lý bằng Hibernate.</p>
 *
 * @reference docs/plan-backend/01-kien-truc-va-stack.md §8
 * @reference docs/04-thiet-ke-backend.md §5.3
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    /** Khoá chính UUID, tự sinh khi persist. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Thời điểm tạo bản ghi. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Thời điểm cập nhật lần cuối. */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Sinh khoá chính UUID trước khi lưu nếu chưa có.
     *
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §8
     */
    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
