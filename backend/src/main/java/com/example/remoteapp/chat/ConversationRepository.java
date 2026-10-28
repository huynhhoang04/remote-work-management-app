package com.example.remoteapp.chat;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho {@link Conversation}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
 */
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /**
     * Kênh GROUP của dự án (mỗi dự án chỉ 1).
     *
     * @param projectId dự án
     * @param type      loại hội thoại
     * @return hội thoại nếu đã tạo
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    Optional<Conversation> findByProjectIdAndType(UUID projectId, ConversationType type);

    /**
     * Hội thoại 1-1 theo khoá cặp user.
     *
     * @param directKey khoá đã sắp xếp
     * @return hội thoại nếu tồn tại
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    Optional<Conversation> findByDirectKey(String directKey);
}
