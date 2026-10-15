package com.example.remoteapp.user;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity người dùng (bảng {@code users}).
 *
 * @reference docs/04-thiet-ke-backend.md §5.2
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A1
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {

    /** Email đăng nhập — lưu đã chuẩn hoá lowercase, unique. */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /** Mật khẩu đã hash BCrypt — không bao giờ trả ra ngoài. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Họ tên hiển thị. */
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** URL/định danh avatar (upload qua module file). */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** Trạng thái tài khoản. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;
}
