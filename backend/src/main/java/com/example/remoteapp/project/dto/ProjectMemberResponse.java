package com.example.remoteapp.project.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO thành viên dự án.
 *
 * @param userId   id người dùng
 * @param fullName họ tên
 * @param email    email
 * @param role     vai trò trong dự án
 * @param joinedAt thời điểm tham gia
 * @reference docs/plan-backend/02-danh-sach-api.md §4
 */
public record ProjectMemberResponse(
        UUID userId,
        String fullName,
        String email,
        String role,
        Instant joinedAt) {
}
