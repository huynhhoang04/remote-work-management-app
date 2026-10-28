package com.example.remoteapp.chat.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Payload đánh dấu đã đọc qua STOMP ({@code /app/conversations.read}).
 *
 * @param conversationId    hội thoại
 * @param lastReadMessageId tin cuối đã đọc
 * @reference docs/plan-backend/02-danh-sach-api.md §12
 */
public record WsReadRequest(
        @NotNull(message = "Hội thoại không được để trống")
        UUID conversationId,

        @NotNull(message = "Tin nhắn cuối đã đọc không được để trống")
        UUID lastReadMessageId) {
}
