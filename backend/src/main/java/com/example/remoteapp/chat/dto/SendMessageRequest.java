package com.example.remoteapp.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request gửi tin nhắn qua REST (fallback khi WebSocket lỗi).
 *
 * @param content         nội dung (1–4000)
 * @param replyToId       tin được trả lời (tuỳ chọn)
 * @param clientMessageId id client sinh để chống trùng khi retry
 * @reference docs/plan-backend/02-danh-sach-api.md §8
 */
public record SendMessageRequest(
        @NotBlank(message = "Nội dung không được để trống")
        @Size(max = 4000, message = "Nội dung tối đa 4000 ký tự")
        String content,

        UUID replyToId,
        UUID clientMessageId) {
}
