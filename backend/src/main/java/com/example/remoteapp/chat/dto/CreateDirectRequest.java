package com.example.remoteapp.chat.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request tạo hội thoại chat 1-1 (idempotent theo cặp user).
 *
 * @param userId người muốn chat cùng
 * @reference docs/plan-backend/02-danh-sach-api.md §8
 */
public record CreateDirectRequest(
        @NotNull(message = "Người chat cùng không được để trống")
        UUID userId) {
}
