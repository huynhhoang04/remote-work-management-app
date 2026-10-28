package com.example.remoteapp.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request tạo nhãn trong dự án.
 *
 * @param name  tên nhãn (bắt buộc, ≤ 50)
 * @param color màu hiển thị (bắt buộc, ≤ 20)
 * @reference docs/plan-backend/02-danh-sach-api.md §5
 */
public record LabelRequest(
        @NotBlank(message = "Tên nhãn không được để trống")
        @Size(max = 50, message = "Tên nhãn tối đa 50 ký tự")
        String name,

        @NotBlank(message = "Màu nhãn không được để trống")
        @Size(max = 20, message = "Màu nhãn tối đa 20 ký tự")
        String color) {
}
