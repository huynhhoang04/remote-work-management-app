package com.example.remoteapp.project.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO link mời dự án (FR-B03 — rút gọn: chỉ link/mã mời).
 *
 * @param id        id link mời
 * @param code      mã mời dùng cho {@code POST /projects/join}
 * @param inviteUrl link mời dạng deep-link cho app mobile
 * @param expiresAt hạn dùng (null = không giới hạn)
 * @reference docs/plan-backend/02-danh-sach-api.md §4
 */
public record InviteResponse(
        UUID id,
        String code,
        String inviteUrl,
        Instant expiresAt) {
}
