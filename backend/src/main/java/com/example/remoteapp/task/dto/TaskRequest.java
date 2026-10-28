package com.example.remoteapp.task.dto;

import com.example.remoteapp.task.TaskPriority;
import com.example.remoteapp.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Request tạo task (FR-B04, FR-B05, FR-B07).
 *
 * @param projectId   dự án chứa task
 * @param title       tiêu đề (bắt buộc, ≤ 200)
 * @param description mô tả (≤ 5000)
 * @param status      trạng thái (mặc định TODO)
 * @param priority    độ ưu tiên (mặc định MEDIUM)
 * @param startDate   ngày bắt đầu (timeline)
 * @param deadline    hạn hoàn thành
 * @param labelIds    nhãn gán kèm
 * @param assigneeIds thành viên được gán
 * @param parentTaskId task cha (checklist/subtask)
 * @param position    vị trí Kanban (mặc định cuối cột)
 * @param version     phiên bản client đang giữ (tuỳ chọn)
 * @reference docs/plan-backend/02-danh-sach-api.md §5
 */
public record TaskRequest(
        @NotNull(message = "Dự án không được để trống")
        UUID projectId,

        @NotBlank(message = "Tiêu đề không được để trống")
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
        UUID parentTaskId,
        Double position,
        Long version) {
}
