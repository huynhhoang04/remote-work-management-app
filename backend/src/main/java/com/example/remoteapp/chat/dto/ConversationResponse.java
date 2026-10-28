package com.example.remoteapp.chat.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO hội thoại trong danh sách chat.
 *
 * @param id               id hội thoại
 * @param type             GROUP hoặc DIRECT
 * @param projectId        dự án (null với chat 1-1)
 * @param name             tên hiển thị (tên dự án hoặc tên người chat)
 * @param lastReadMessageId mốc đọc cuối của người dùng hiện tại
 * @param unreadCount      số tin chưa đọc
 * @param lastMessage      tin nhắn mới nhất (null nếu chưa có)
 * @param createdAt        thời điểm tạo hội thoại
 * @reference docs/plan-backend/02-danh-sach-api.md §8
 */
public record ConversationResponse(UUID id, String type, UUID projectId, String name,
                                   UUID lastReadMessageId, long unreadCount,
                                   MessageResponse lastMessage, Instant createdAt) {
}
