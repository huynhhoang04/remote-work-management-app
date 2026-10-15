package com.example.remoteapp.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request tạo/sửa dự án (FR-B02).
 *
 * @param name        tên dự án 1–100 ký tự
 * @param description mô tả tối đa 1000 ký tự (tuỳ chọn)
 * @reference docs/plan-backend/02-danh-sach-api.md §4
 */
public record ProjectRequest(
        @NotBlank(message = "Tên dự án không được để trống")
        @Size(max = 100, message = "Tên dự án tối đa 100 ký tự")
        String name,

        @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
        String description) {
}
