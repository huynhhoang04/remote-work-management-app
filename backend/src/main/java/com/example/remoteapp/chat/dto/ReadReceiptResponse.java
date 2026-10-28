package com.example.remoteapp.chat.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Sự kiện read receipt broadcast trong hội thoại.
 *
 * @param conversationId    hội thoại
 * @param userId            người đọc
 * @param lastReadMessageId tin cuối đã đọc
 * @param readAt            thời điểm đọc
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2
 */
public record ReadReceiptResponse(UUID conversationId, UUID userId, UUID lastReadMessageId,
                                  Instant readAt) {
}
