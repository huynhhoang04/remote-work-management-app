package com.example.remoteapp.user.dto;

import jakarta.validation.constraints.Size;

/**
 * Request cập nhật hồ sơ (FR-A04) — mọi trường đều tuỳ chọn.
 *
 * @param fullName  họ tên mới (null = giữ nguyên)
 * @param avatarUrl avatar mới (null = giữ nguyên; upload qua module file)
 * @reference docs/plan-backend/02-danh-sach-api.md §3
 */
public record UpdateProfileRequest(
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String fullName,

        @Size(max = 500, message = "Đường dẫn avatar tối đa 500 ký tự")
        String avatarUrl) {
}
