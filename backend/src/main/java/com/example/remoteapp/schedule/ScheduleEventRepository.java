package com.example.remoteapp.schedule;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho {@link ScheduleEvent}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1
 */
public interface ScheduleEventRepository extends JpaRepository<ScheduleEvent, UUID> {

    /**
     * Sự kiện của một người trong khoảng thời gian.
     *
     * @param userId người dùng
     * @param from   từ thời điểm
     * @param to     đến thời điểm
     * @return danh sách sự kiện theo thời gian bắt đầu
     * @reference docs/plan-backend/02-danh-sach-api.md §6
     */
    List<ScheduleEvent> findByUserIdAndStartTimeBetweenOrderByStartTimeAsc(UUID userId,
                                                                          Instant from, Instant to);

    /**
     * Tìm sự kiện thuộc đúng chủ sở hữu.
     *
     * @param id     id sự kiện
     * @param userId chủ sở hữu
     * @return sự kiện nếu khớp
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1
     */
    Optional<ScheduleEvent> findByIdAndUserId(UUID id, UUID userId);
}
