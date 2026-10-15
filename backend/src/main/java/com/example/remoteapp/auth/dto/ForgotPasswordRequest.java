package com.example.remoteapp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request yêu cầu đặt lại mật khẩu (FR-A03).
 *
 * @param email email cần khôi phục — endpoint luôn trả 204 kể cả email không tồn tại
 * @reference docs/plan-backend/02-danh-sach-api.md §2
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email) {
}
