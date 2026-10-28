package com.example.remoteapp.task.dto;

import com.example.remoteapp.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request cập nhật vị trí Kanban (kéo thả).
 *
 * @param status   cột đích
 * @param position vị trí mới trong cột
 * @reference docs/plan-backend/02-danh-sach-api.md §5
 */
public record PositionRequest(
        @NotNull(message = "Trạng thái không được để trống")
        TaskStatus status,

        @NotNull(message = "Vị trí không được để trống")
        Double position) {
}
