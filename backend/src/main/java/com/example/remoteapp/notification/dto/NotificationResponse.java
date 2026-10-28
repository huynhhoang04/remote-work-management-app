package com.example.remoteapp.notification.dto;

import com.example.remoteapp.notification.Notification;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO thông báo trong trung tâm thông báo.
 *
 * @param id        id thông báo
 * @param type      loại thông báo
 * @param title     tiêu đề
 * @param body      nội dung
 * @param refType   loại đối tượng tham chiếu
 * @param refId     id đối tượng tham chiếu
 * @param readAt    thời điểm đọc (null = chưa đọc)
 * @param createdAt thời điểm tạo
 * @reference docs/plan-backend/02-danh-sach-api.md §9
 */
public record NotificationResponse(UUID id, String type, String title, String body,
                                   String refType, UUID refId, Instant readAt, Instant createdAt) {

    /**
     * Ánh xạ entity sang DTO.
     *
     * @param notification entity thông báo
     * @return DTO phản hồi
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1 (mapping thủ công)
     */
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType().name(),
                notification.getTitle(), notification.getBody(), notification.getRefType(),
                notification.getRefId(), notification.getReadAt(), notification.getCreatedAt());
    }
}
