package com.example.remoteapp.task.dto;

import com.example.remoteapp.task.TaskPriority;
import com.example.remoteapp.task.TaskStatus;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Request PATCH task — trường null nghĩa là giữ nguyên (FR-B04).
 *
 * @param title       tiêu đề mới (null = giữ nguyên)
 * @param description mô tả mới (null = giữ nguyên)
 * @param status      trạng thái mới
 * @param priority    độ ưu tiên mới
 * @param startDate   ngày bắt đầu mới
 * @param deadline    hạn mới
 * @param labelIds    danh sách nhãn đầy đủ (null = giữ nguyên)
 * @param assigneeIds danh sách assignee đầy đủ (null = giữ nguyên)
 * @param position    vị trí Kanban mới
 * @param version     phiên bản client đang giữ (chống ghi đè lạc hậu)
 * @reference docs/plan-backend/02-danh-sach-api.md §5
 */
public record TaskUpdateRequest(
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        String title,

        @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
        String description,

        TaskStatus status,
        TaskPriority priority,
        Instant startDate,
        Instant deadline,
        List<UUID> labelIds,
        List<UUID> assigneeIds,
        Double position,
        Long version) {
}
