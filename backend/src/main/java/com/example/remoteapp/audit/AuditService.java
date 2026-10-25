package com.example.remoteapp.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi và truy vấn audit log — module "leaf", mọi module nghiệp vụ đều gọi được
 * (plan 01 §4.2).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §G
 */
@Service
public class AuditService {

    private final AuditRepository auditRepository;

    /**
     * Khởi tạo service (constructor injection tường minh).
     *
     * @param auditRepository repository audit log
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Ghi một hành động vào audit log (đồng bộ trong transaction của request).
     *
     * @param actorId    người thực hiện (null nếu hệ thống)
     * @param projectId  dự án liên quan (null nếu cấp tài khoản)
     * @param action     hành động
     * @param entityType loại đối tượng
     * @param entityId   id đối tượng
     * @param metadata   chi tiết bổ sung (chuỗi ngắn, có thể null)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §G
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID actorId, UUID projectId, AuditAction action, String entityType,
                       UUID entityId, String metadata) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setProjectId(projectId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setMetadata(metadata);
        auditRepository.save(log);
    }

    /**
     * Phân trang nhật ký của dự án, lọc theo hành động (tuỳ chọn).
     *
     * @param projectId dự án cần xem
     * @param action    hành động lọc (null = tất cả)
     * @param pageable  phân trang
     * @return trang audit log (mới nhất trước)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §G
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> listByProject(UUID projectId, AuditAction action, Pageable pageable) {
        if (action == null) {
            return auditRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
        }
        return auditRepository.findByProjectIdAndActionOrderByCreatedAtDesc(projectId, action, pageable);
    }
}
