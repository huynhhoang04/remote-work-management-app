package com.example.remoteapp.task.dto;

import com.example.remoteapp.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request đổi trạng thái task (kéo Kanban).
 *
 * @param status trạng thái mới
 * @reference docs/plan-backend/02-danh-sach-api.md §5
 */
public record StatusRequest(@NotNull(message = "Trạng thái không được để trống") TaskStatus status) {
}
