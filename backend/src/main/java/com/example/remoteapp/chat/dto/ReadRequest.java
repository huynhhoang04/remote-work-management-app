package com.example.remoteapp.chat.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request đánh dấu đã đọc tới một tin nhắn.
 *
 * @param lastReadMessageId tin cuối đã đọc
 * @reference docs/plan-backend/02-danh-sach-api.md §8
 */
public record ReadRequest(
        @NotNull(message = "Tin nhắn cuối đã đọc không được để trống")
        UUID lastReadMessageId) {
}
