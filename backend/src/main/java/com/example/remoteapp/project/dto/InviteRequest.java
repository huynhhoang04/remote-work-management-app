package com.example.remoteapp.project.dto;

import java.time.Instant;

/**
 * Request tạo link mời dự án.
 *
 * @param expiresAt hạn dùng của link (tuỳ chọn — null = không giới hạn)
 * @reference docs/plan-backend/02-danh-sach-api.md §4
 */
public record InviteRequest(Instant expiresAt) {
}
