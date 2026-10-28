package com.example.remoteapp.config;

import com.example.remoteapp.notification.FcmPushSender;
import com.example.remoteapp.notification.NoopPushSender;
import com.example.remoteapp.notification.PushSender;
import com.example.remoteapp.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chọn implementation gửi push theo {@code app.fcm.enabled}.
 *
 * <p>Biến rỗng/giá trị lạ đều rơi về no-op để backend không fail vì thiếu Firebase.</p>
 *
 * @reference docs/plan-deploy/02-dich-vu-ngoai.md §8 (fallback matrix)
 */
@Configuration
public class PushConfig {

    /**
     * Tạo bean PushSender: FCM khi bật rõ ràng, còn lại no-op.
     *
     * @param enabled          cờ bật FCM
     * @param credentialsBase64 service account JSON base64
     * @param userService      xoá token hỏng
     * @return implementation tương ứng
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §3
     */
    @Bean
    public PushSender pushSender(@Value("${app.fcm.enabled:false}") String enabled,
                                 @Value("${app.fcm.credentials-base64:}") String credentialsBase64,
                                 UserService userService) {
        if ("true".equalsIgnoreCase(enabled.trim())) {
            return new FcmPushSender(userService, credentialsBase64);
        }
        return new NoopPushSender();
    }
}
