package com.example.remoteapp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request đổi mật khẩu khi đã đăng nhập (FR-A04).
 *
 * @param oldPassword mật khẩu hiện tại (xác nhận chủ sở hữu)
 * @param newPassword mật khẩu mới 8–72 ký tự
 * @reference docs/plan-backend/02-danh-sach-api.md §3
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        String oldPassword,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
        String newPassword) {
}
