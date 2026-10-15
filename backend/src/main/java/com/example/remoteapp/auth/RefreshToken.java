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
 * Refresh token đã phát hành — chỉ lưu **hash** của token (không lưu bản gốc).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
 * @reference docs/04-thiet-ke-backend.md §10
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshToken extends BaseEntity {

    /** Chủ sở hữu token. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Hash SHA-256 của refresh token. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** Mã thiết bị (tuỳ chọn) để quản lý phiên theo thiết bị. */
    @Column(name = "device_id", length = 100)
    private String deviceId;

    /** Thời điểm hết hạn. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Đã bị thu hồi (logout / rotate / đổi mật khẩu) hay chưa. */
    @Column(name = "revoked", nullable = false)
    private boolean revoked;
}
