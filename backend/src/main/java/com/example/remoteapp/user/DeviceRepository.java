package com.example.remoteapp.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho {@link Device}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A5
 */
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    /**
     * Tìm thiết bị theo FCM token (upsert khi đăng ký token).
     *
     * @param fcmToken FCM token từ client
     * @return thiết bị nếu token đã được đăng ký
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A5
     */
    Optional<Device> findByFcmToken(String fcmToken);

    /**
     * Liệt kê thiết bị của một người dùng (gửi push tới tất cả).
     *
     * @param userId chủ thiết bị
     * @return danh sách thiết bị
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E2
     */
    List<Device> findByUserId(UUID userId);
}
