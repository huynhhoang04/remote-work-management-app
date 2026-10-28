package com.example.remoteapp.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho {@link ConversationMember}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
 */
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {

    /**
     * Các hội thoại người dùng tham gia (dùng cho chat 1-1).
     *
     * @param userId người dùng
     * @return danh sách quan hệ thành viên
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    List<ConversationMember> findByUserId(UUID userId);

    /**
     * Quan hệ thành viên của một người trong hội thoại.
     *
     * @param conversationId hội thoại
     * @param userId         người dùng
     * @return quan hệ nếu có
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    Optional<ConversationMember> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    /**
     * Kiểm tra người dùng có trong hội thoại không.
     *
     * @param conversationId hội thoại
     * @param userId         người dùng
     * @return true nếu là thành viên
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    /**
     * Toàn bộ thành viên của hội thoại.
     *
     * @param conversationId hội thoại
     * @return danh sách thành viên
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    List<ConversationMember> findByConversationId(UUID conversationId);
}
