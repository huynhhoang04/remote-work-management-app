package com.example.remoteapp.chat.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO tin nhắn.
 *
 * @param id              id tin nhắn
 * @param conversationId  hội thoại
 * @param senderId        người gửi
 * @param senderName      họ tên người gửi
 * @param content         nội dung
 * @param replyToId       tin được trả lời
 * @param clientMessageId id client sinh (idempotent)
 * @param createdAt       thời điểm gửi
 * @reference docs/plan-backend/02-danh-sach-api.md §8
 */
public record MessageResponse(UUID id, UUID conversationId, UUID senderId, String senderName,
                              String content, UUID replyToId, UUID clientMessageId,
                              Instant createdAt) {
}
