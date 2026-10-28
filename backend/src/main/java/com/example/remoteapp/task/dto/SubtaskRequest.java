package com.example.remoteapp.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request tạo checklist con.
 *
 * @param title tiêu đề subtask (bắt buộc, ≤ 200)
 * @reference docs/plan-backend/02-danh-sach-api.md §5
 */
public record SubtaskRequest(
        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        String title) {
}
