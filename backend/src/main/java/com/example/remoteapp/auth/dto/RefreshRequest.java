package com.example.remoteapp.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request làm mới / thu hồi phiên đăng nhập.
 *
 * @param refreshToken refresh token client đang giữ
 * @reference docs/plan-backend/02-danh-sach-api.md §2
 */
public record RefreshRequest(
        @NotBlank(message = "Refresh token không được để trống")
        String refreshToken) {
}
