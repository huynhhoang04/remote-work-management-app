package com.example.remoteapp.project.dto;

import com.example.remoteapp.project.ProjectRole;
import jakarta.validation.constraints.NotNull;

/**
 * Request đổi vai trò thành viên dự án (FR-G01).
 *
 * @param role vai trò mới (không nhận OWNER — xem ProjectService.changeRole)
 * @reference docs/plan-backend/02-danh-sach-api.md §4
 */
public record RoleRequest(
        @NotNull(message = "Vai trò không được để trống")
        ProjectRole role) {
}
