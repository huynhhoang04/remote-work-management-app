package com.example.remoteapp.auth;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Token đặt lại mật khẩu — chỉ lưu **hash**, dùng một lần, có hạn.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
public class PasswordResetToken extends BaseEntity {

    /** Người dùng yêu cầu đặt lại mật khẩu. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Hash SHA-256 của token gửi qua email. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** Thời điểm hết hạn (mặc định 30 phút). */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Thời điểm đã sử dụng (null = chưa dùng). */
    @Column(name = "used_at")
    private Instant usedAt;
}
