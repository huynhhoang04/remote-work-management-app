package com.example.remoteapp.chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho {@link Message}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2
 */
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Trang tin nhắn mới nhất (phân trang ngược, không có mốc before).
     *
     * @param conversationId hội thoại
     * @param pageable       phân trang
     * @return danh sách mới nhất trước
     * @reference docs/plan-backend/02-danh-sach-api.md §8
     */
    List<Message> findByConversationIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID conversationId, Pageable pageable);

    /**
     * Trang tin nhắn trước một mốc thời gian (phân trang ngược).
     *
     * @param conversationId hội thoại
     * @param before         mốc thời gian
     * @param pageable       phân trang
     * @return danh sách mới nhất trước trong khoảng
     * @reference docs/plan-backend/02-danh-sach-api.md §8
     */
    List<Message> findByConversationIdAndDeletedAtIsNullAndCreatedAtBeforeOrderByCreatedAtDesc(
            UUID conversationId, Instant before, Pageable pageable);

    /**
     * Tìm tin theo id client (idempotent khi retry).
     *
     * @param conversationId  hội thoại
     * @param clientMessageId id client sinh
     * @return tin nhắn nếu đã tồn tại
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2
     */
    Optional<Message> findByConversationIdAndClientMessageId(UUID conversationId,
                                                            UUID clientMessageId);

    /**
     * Tìm tin chưa bị xoá.
     *
     * @param id id tin nhắn
     * @return tin nhắn nếu tồn tại
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2
     */
    Optional<Message> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * Tin nhắn mới nhất của hội thoại.
     *
     * @param conversationId hội thoại
     * @return tin mới nhất nếu có
     * @reference docs/plan-backend/02-danh-sach-api.md §8
     */
    Optional<Message> findFirstByConversationIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID conversationId);

    /**
     * Đếm tin của người khác trong hội thoại (khi chưa đọc gì).
     *
     * @param conversationId hội thoại
     * @param senderId       người dùng hiện tại
     * @return số tin chưa đọc
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    long countByConversationIdAndSenderIdNot(UUID conversationId, UUID senderId);

    /**
     * Đếm tin của người khác sau một mốc thời gian (unread count).
     *
     * @param conversationId hội thoại
     * @param senderId       người dùng hiện tại
     * @param after          mốc đọc cuối
     * @return số tin chưa đọc
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    long countByConversationIdAndSenderIdNotAndCreatedAtAfter(UUID conversationId, UUID senderId,
                                                              Instant after);
}
