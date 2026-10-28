package com.example.remoteapp.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Request tạo/sửa sự kiện lịch cá nhân.
 *
 * @param title       tiêu đề (bắt buộc, ≤ 200)
 * @param description mô tả (≤ 2000)
 * @param startTime   thời điểm bắt đầu (bắt buộc)
 * @param endTime     thời điểm kết thúc (bắt buộc, sau startTime)
 * @param taskId      task liên kết (tuỳ chọn)
 * @reference docs/plan-backend/02-danh-sach-api.md §6
 */
public record ScheduleEventRequest(
        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        String title,

        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        String description,

        @NotNull(message = "Thời điểm bắt đầu không được để trống")
        Instant startTime,

        @NotNull(message = "Thời điểm kết thúc không được để trống")
        Instant endTime,

        UUID taskId) {
}
