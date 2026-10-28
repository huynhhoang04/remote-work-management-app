package com.example.remoteapp.task.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Request gán lại danh sách người thực hiện (danh sách đầy đủ).
 *
 * @param assigneeIds id thành viên dự án (có thể rỗng để bỏ gán hết)
 * @reference docs/plan-backend/02-danh-sach-api.md §5
 */
public record AssigneesRequest(
        @NotNull(message = "Danh sách assignee không được để trống")
        List<UUID> assigneeIds) {
}
