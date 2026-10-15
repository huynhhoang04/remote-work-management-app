package com.example.remoteapp.project.dto;

import com.example.remoteapp.project.Project;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO dự án trả về client.
 *
 * @param id          id dự án
 * @param name        tên dự án
 * @param description mô tả
 * @param ownerId     chủ dự án
 * @param role        vai trò của người yêu cầu (tiện cho UI ẩn/hiện nút)
 * @param createdAt   thời điểm tạo
 * @reference docs/plan-backend/02-danh-sach-api.md §4
 */
public record ProjectResponse(
        UUID id,
        String name,
        String description,
        UUID ownerId,
        String role,
        Instant createdAt) {

    /**
     * Ánh xạ entity sang DTO kèm vai trò của người xem.
     *
     * @param project dự án
     * @param role    vai trò của người yêu cầu
     * @return DTO phản hồi
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public static ProjectResponse from(Project project, String role) {
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(),
                project.getOwnerId(), role, project.getCreatedAt());
    }
}
