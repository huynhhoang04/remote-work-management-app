package com.example.remoteapp.notification;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.notification.dto.NotificationResponse;
import com.example.remoteapp.user.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ trung tâm thông báo + đẩy push/WS (plan 03 §E2).
 *
 * <p>Thứ tự: lưu DB (nguồn sự thật) → đẩy WebSocket nếu app mở → gửi FCM nếu có
 * credentials. Lỗi push không làm hỏng giao dịch chính.</p>
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E2
 */
@Slf4j
@Service
public class NotificationService {

    private static final String QUEUE_NOTIFICATIONS = "/queue/notifications";

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final PushSender pushSender;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Khởi tạo service (constructor injection tường minh).
     *
     * @param notificationRepository repository thông báo
     * @param userService            lấy FCM token của người nhận
     * @param pushSender             cổng gửi push (FCM/no-op)
     * @param messagingTemplate      gửi realtime qua WebSocket
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public NotificationService(NotificationRepository notificationRepository, UserService userService,
                               PushSender pushSender, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
        this.pushSender = pushSender;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Tạo thông báo cho một người (bỏ qua nếu trùng {@code dedupeKey}), kèm WS + push.
     *
     * @param userId    người nhận
     * @param type      loại thông báo
     * @param title     tiêu đề
     * @param body      nội dung
     * @param refType   loại đối tượng tham chiếu
     * @param refId     id đối tượng tham chiếu
     * @param dedupeKey khoá chống trùng (null nếu không cần)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1, §E2
     */
    @Transactional
    public void notify(UUID userId, NotificationType type, String title, String body,
                       String refType, UUID refId, String dedupeKey) {
        if (dedupeKey != null && notificationRepository.existsByDedupeKey(dedupeKey)) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRefType(refType);
        notification.setRefId(refId);
        notification.setDedupeKey(dedupeKey);
        notificationRepository.save(notification);

        NotificationResponse payload = NotificationResponse.from(notification);
        sendRealtime(userId, payload);
        push(userId, title, body, refType, refId);
    }

    /**
     * Phân trang thông báo của người dùng.
     *
     * @param userId   người nhận
     * @param pageable phân trang
     * @return trang thông báo (mới nhất trước)
     * @reference docs/plan-backend/02-danh-sach-api.md §9
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    /**
     * Đếm thông báo chưa đọc.
     *
     * @param userId người nhận
     * @return số chưa đọc
     * @reference docs/plan-backend/02-danh-sach-api.md §9
     */
    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    /**
     * Đánh dấu một thông báo đã đọc (chỉ của chính mình).
     *
     * @param userId người nhận
     * @param id     id thông báo
     * @throws AppException NOTIFICATION_NOT_FOUND nếu không thuộc người dùng
     * @reference docs/plan-backend/02-danh-sach-api.md §9
     */
    @Transactional
    public void markRead(UUID userId, UUID id) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND,
                        "Không tìm thấy thông báo"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    /**
     * Đánh dấu đã đọc toàn bộ thông báo của người dùng.
     *
     * @param userId người nhận
     * @reference docs/plan-backend/02-danh-sach-api.md §9
     */
    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId, Instant.now());
    }

    /**
     * Đẩy thông báo realtime tới phiên WebSocket của người dùng (nếu đang mở).
     *
     * @param userId  người nhận
     * @param payload DTO thông báo
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2
     */
    private void sendRealtime(UUID userId, NotificationResponse payload) {
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), QUEUE_NOTIFICATIONS, payload);
        } catch (RuntimeException ex) {
            log.debug("Không gửi được WS notification cho {}: {}", userId, ex.getMessage());
        }
    }

    /**
     * Gửi push qua FCM tới mọi thiết bị của người nhận (bỏ qua nếu chưa có token).
     *
     * @param userId người nhận
     * @param title  tiêu đề
     * @param body   nội dung
     * @param refType loại đối tượng tham chiếu
     * @param refId  id đối tượng tham chiếu
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E2
     */
    private void push(UUID userId, String title, String body, String refType, UUID refId) {
        List<String> tokens = userService.listFcmTokens(userId);
        if (tokens.isEmpty()) {
            return;
        }
        Map<String, String> data = refId == null
                ? Map.of()
                : Map.of("refType", refType == null ? "" : refType, "refId", refId.toString());
        try {
            pushSender.push(tokens, title, body, data);
        } catch (RuntimeException ex) {
            log.warn("Gửi push thất bại cho {}: {}", userId, ex.getMessage());
        }
    }
}
