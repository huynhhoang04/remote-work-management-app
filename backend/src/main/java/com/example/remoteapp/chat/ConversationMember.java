package com.example.remoteapp.chat;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Thành viên hội thoại + mốc đọc cuối (read receipt, plan 03 §F1).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
 * @reference docs/04-thiet-ke-backend.md §5.2
 */
@Entity
@Table(name = "conversation_members")
@Getter
@Setter
public class ConversationMember extends BaseEntity {

    /** Hội thoại. */
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    /** Người tham gia. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Tin nhắn cuối đã đọc (null = chưa đọc gì). */
    @Column(name = "last_read_message_id")
    private UUID lastReadMessageId;
}
