package com.example.remoteapp.auth.dto;

import com.example.remoteapp.user.dto.UserResponse;
import java.time.Instant;

/**
 * Response đăng nhập / refresh token thành công.
 *
 * @param accessToken          JWT access token (Bearer)
 * @param refreshToken         refresh token xoay vòng (client lưu an toàn)
 * @param accessTokenExpiresAt thời điểm access token hết hạn
 * @param user                 hồ sơ người dùng
 * @reference docs/plan-backend/02-danh-sach-api.md §2
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        UserResponse user) {
}
