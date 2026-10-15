package com.example.remoteapp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request đăng ký tài khoản (FR-A01).
 *
 * @param email    email hợp lệ, chưa được đăng ký
 * @param password mật khẩu 8–72 ký tự (giới hạn trên theo BCrypt)
 * @param fullName họ tên 1–100 ký tự
 * @reference docs/plan-backend/02-danh-sach-api.md §2
 */
public record RegisterRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
        String password,

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String fullName) {
}
