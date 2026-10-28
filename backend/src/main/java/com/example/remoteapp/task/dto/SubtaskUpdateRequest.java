package com.example.remoteapp.task.dto;

import jakarta.validation.constraints.Size;

/**
 * Request cập nhật checklist con.
 *
 * @param title tiêu đề mới (null = giữ nguyên)
 * @param done  đánh dấu hoàn thành (null = giữ nguyên)
 * @reference docs/plan-backend/02-danh-sach-api.md §5
 */
public record SubtaskUpdateRequest(
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        String title,

        Boolean done) {
}
