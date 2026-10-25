package com.example.remoteapp.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho {@link AuditLog}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §G
 */
public interface AuditRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Phân trang nhật ký của một dự án (mới nhất trước).
     *
     * @param projectId dự án cần xem
     * @param pageable  phân trang
     * @return trang audit log
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §G
     */
    Page<AuditLog> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);

    /**
     * Phân trang nhật ký của dự án theo loại hành động.
     *
     * @param projectId dự án cần xem
     * @param action    hành động lọc
     * @param pageable  phân trang
     * @return trang audit log
     * @reference docs/plan-backend/02-danh-sach-api.md §10
     */
    Page<AuditLog> findByProjectIdAndActionOrderByCreatedAtDesc(UUID projectId, AuditAction action,
                                                                 Pageable pageable);
}
