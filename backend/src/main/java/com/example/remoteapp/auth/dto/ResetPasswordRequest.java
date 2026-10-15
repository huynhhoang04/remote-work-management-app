package com.example.remoteapp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request đặt lại mật khẩu bằng token trong email.
 *
 * @param token       token nhận từ link email (dùng một lần)
 * @param newPassword mật khẩu mới 8–72 ký tự
 * @reference docs/plan-backend/02-danh-sach-api.md §2
 */
public record ResetPasswordRequest(
        @NotBlank(message = "Token không được để trống")
        String token,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
        String newPassword) {
}
