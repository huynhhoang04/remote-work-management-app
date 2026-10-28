package com.example.remoteapp.notification;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository cho {@link Notification}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E2
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Phân trang thông báo của một người (mới nhất trước).
     *
     * @param userId   người nhận
     * @param pageable phân trang
     * @return trang thông báo
     * @reference docs/plan-backend/02-danh-sach-api.md §9
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Đếm thông báo chưa đọc.
     *
     * @param userId người nhận
     * @return số chưa đọc
     * @reference docs/plan-backend/02-danh-sach-api.md §9
     */
    long countByUserIdAndReadAtIsNull(UUID userId);

    /**
     * Tìm thông báo thuộc đúng người nhận (chống đọc hộ).
     *
     * @param id     id thông báo
     * @param userId người nhận
     * @return thông báo nếu khớp
     * @reference docs/plan-backend/02-danh-sach-api.md §9
     */
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Kiểm tra khoá chống trùng đã tồn tại chưa.
     *
     * @param dedupeKey khoá chống trùng
     * @return true nếu đã có
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1
     */
    boolean existsByDedupeKey(String dedupeKey);

    /**
     * Đánh dấu đã đọc toàn bộ thông báo chưa đọc của người dùng.
     *
     * @param userId người nhận
     * @param now    thời điểm đọc
     * @return số bản ghi cập nhật
     * @reference docs/plan-backend/02-danh-sach-api.md §9
     */
    @Modifying
    @Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("now") Instant now);
}
