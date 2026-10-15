package com.example.remoteapp.audit.dto;

import com.example.remoteapp.audit.AuditLog;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO một dòng audit log (chỉ Owner dự án xem được).
 *
 * @param id         id bản ghi
 * @param actorId    người thực hiện
 * @param action     hành động
 * @param entityType loại đối tượng
 * @param entityId   id đối tượng
 * @param metadata   chi tiết bổ sung
 * @param createdAt  thời điểm ghi
 * @reference docs/plan-backend/02-danh-sach-api.md §10
 */
public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String action,
        String entityType,
        UUID entityId,
        String metadata,
        Instant createdAt) {

    /**
     * Ánh xạ entity sang DTO.
     *
     * @param log bản ghi audit
     * @return DTO phản hồi
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getActorId(), log.getAction().name(),
                log.getEntityType(), log.getEntityId(), log.getMetadata(), log.getCreatedAt());
    }
}
