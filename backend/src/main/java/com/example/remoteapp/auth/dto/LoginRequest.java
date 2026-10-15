package com.example.remoteapp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request đăng nhập (FR-A02).
 *
 * @param email    email đã đăng ký
 * @param password mật khẩu
 * @reference docs/plan-backend/02-danh-sach-api.md §2
 */
public record LoginRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password) {
}
