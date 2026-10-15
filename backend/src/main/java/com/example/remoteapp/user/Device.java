package com.example.remoteapp.user;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Thiết bị di động đã đăng ký nhận push FCM (bảng {@code devices}).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A5
 * @reference docs/04-thiet-ke-backend.md §5.2
 */
@Entity
@Table(name = "devices")
@Getter
@Setter
public class Device extends BaseEntity {

    /** Chủ thiết bị. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** FCM token của thiết bị — unique, upsert theo token. */
    @Column(name = "fcm_token", nullable = false, unique = true, length = 500)
    private String fcmToken;

    /** Nền tảng: ANDROID / IOS. */
    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    /** Lần hoạt động gần nhất của thiết bị. */
    @Column(name = "last_active_at")
    private Instant lastActiveAt;
}
