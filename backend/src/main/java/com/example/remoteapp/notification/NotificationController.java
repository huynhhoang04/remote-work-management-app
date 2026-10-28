package com.example.remoteapp.notification;

import com.example.remoteapp.common.SecurityUtils;
import com.example.remoteapp.notification.dto.NotificationResponse;
import com.example.remoteapp.notification.dto.UnreadCountResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller trung tâm thông báo (plan 02 §9).
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §9
 */
@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Khởi tạo controller (constructor injection tường minh).
     *
     * @param notificationService service thông báo
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Danh sách thông báo của người dùng đang đăng nhập.
     *
     * @param pageable phân trang
     * @return trang thông báo
     * @reference docs/plan-backend/02-danh-sach-api.md §9
     */
    @GetMapping("/notifications")
    public ResponseEntity<Page<NotificationResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(notificationService.list(SecurityUtils.currentUserId(), pageable));
    }

    /**
     * Đếm số thông báo chưa đọc.
     *
     * @return số chưa đọc
     * @reference docs/plan-backend/02-danh-sach-api.md §9
     */
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount() {
        return ResponseEntity.ok(new UnreadCountResponse(
                notificationService.unreadCount(SecurityUtils.currentUserId())));
    }

    /**
     * Đánh dấu một thông báo đã đọc.
     *
     * @param id id thông báo
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §9
     */
    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        notificationService.markRead(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Đánh dấu đã đọc toàn bộ thông báo.
     *
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §9
     */
    @PatchMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
