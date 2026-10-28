package com.example.remoteapp.notification;

import com.example.remoteapp.user.UserService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Gửi push qua Firebase Cloud Messaging — chỉ bật khi {@code FCM_ENABLED=true}
 * và có service account base64. Bean được tạo bởi {@code PushConfig}.
 *
 * <p>Khởi tạo lỗi (JSON hỏng, thiếu quyền) không làm sập ứng dụng: chỉ log và
 * chuyển sang chế độ không gửi.</p>
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E2
 * @reference docs/plan-deploy/02-dich-vu-ngoai.md §3
 */
@Slf4j
public class FcmPushSender implements PushSender {

    private final UserService userService;
    private final FirebaseMessaging messaging;

    /**
     * Khởi tạo FirebaseApp từ credentials base64 (constructor injection tường minh).
     *
     * @param userService       service user (xoá token hỏng)
     * @param credentialsBase64 service account JSON đã base64
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §3
     */
    public FcmPushSender(UserService userService, String credentialsBase64) {
        this.userService = userService;
        this.messaging = initMessaging(credentialsBase64);
    }

    /**
     * Gửi multicast tới các thiết bị, xoá token FCM không còn hiệu lực.
     *
     * @param tokens danh sách token
     * @param title  tiêu đề
     * @param body   nội dung
     * @param data   dữ liệu kèm theo
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E2
     */
    @Override
    public void push(List<String> tokens, String title, String body, Map<String, String> data) {
        if (messaging == null || tokens == null || tokens.isEmpty()) {
            return;
        }
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data)
                .build();
        try {
            BatchResponse response = messaging.sendEachForMulticast(message);
            removeInvalidTokens(tokens, response);
        } catch (FirebaseMessagingException ex) {
            log.warn("Gửi FCM thất bại: {}", ex.getMessage());
        }
    }

    /**
     * Xoá các token mà FCM báo không còn hiệu lực.
     *
     * @param tokens   token đã gửi
     * @param response kết quả gửi từng token
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A5
     */
    private void removeInvalidTokens(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size() && i < tokens.size(); i++) {
            SendResponse item = responses.get(i);
            if (item.isSuccessful() || item.getException() == null) {
                continue;
            }
            MessagingErrorCode code = item.getException().getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                userService.removeDeviceByToken(tokens.get(i));
            }
        }
    }

    /**
     * Khởi tạo FirebaseMessaging, trả null nếu cấu hình lỗi.
     *
     * @param credentialsBase64 service account JSON đã base64
     * @return FirebaseMessaging hoặc null
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §3
     */
    private FirebaseMessaging initMessaging(String credentialsBase64) {
        if (credentialsBase64 == null || credentialsBase64.isBlank()) {
            log.warn("FCM_ENABLED=true nhưng thiếu FCM_CREDENTIALS_BASE64 — tắt push");
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(credentialsBase64);
            GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            return FirebaseMessaging.getInstance(app);
        } catch (Exception ex) {
            log.error("Không khởi tạo được Firebase — push bị tắt: {}", ex.getMessage());
            return null;
        }
    }
}
