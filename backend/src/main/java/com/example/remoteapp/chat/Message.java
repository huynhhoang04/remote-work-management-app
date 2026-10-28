package com.example.remoteapp.chat;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Tin nhắn trong hội thoại (plan 03 §F2).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2
 * @reference docs/04-thiet-ke-backend.md §5.2
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
public class Message extends BaseEntity {

    /** Hội thoại chứa tin. */
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    /** Người gửi. */
    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    /** Nội dung (≤ 4000). */
    @Column(nullable = false, length = 4000)
    private String content;

    /** Tin được trả lời (reply). */
    @Column(name = "reply_to_id")
    private UUID replyToId;

    /** Id client sinh — chống trùng khi retry. */
    @Column(name = "client_message_id")
    private UUID clientMessageId;

    /** Soft delete. */
    @Column(name = "deleted_at")
    private Instant deletedAt;
}
