package com.example.remoteapp.chat;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Hội thoại chat: kênh GROUP theo dự án hoặc DIRECT 1-1 (plan 03 §F1).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
 * @reference docs/04-thiet-ke-backend.md §5.2
 */
@Entity
@Table(name = "conversations")
@Getter
@Setter
public class Conversation extends BaseEntity {

    /** Dự án (null với chat 1-1). */
    @Column(name = "project_id")
    private UUID projectId;

    /** Loại hội thoại. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ConversationType type;

    /** Khoá cặp user đã sắp xếp cho chat 1-1 (idempotent). */
    @Column(name = "direct_key", length = 80)
    private String directKey;
}
