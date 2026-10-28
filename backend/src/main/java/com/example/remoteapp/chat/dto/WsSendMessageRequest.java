package com.example.remoteapp.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Payload gửi tin nhắn qua STOMP ({@code /app/chat.send}).
 *
 * @param conversationId  hội thoại
 * @param content         nội dung (1–4000)
 * @param replyToId       tin được trả lời
 * @param clientMessageId id client sinh để chống trùng
 * @reference docs/plan-backend/02-danh-sach-api.md §12
 */
public record WsSendMessageRequest(
        @NotNull(message = "Hội thoại không được để trống")
        UUID conversationId,

        @NotBlank(message = "Nội dung không được để trống")
        @Size(max = 4000, message = "Nội dung tối đa 4000 ký tự")
        String content,

        UUID replyToId,
        UUID clientMessageId) {
}
