package com.example.remoteapp.schedule.dto;

import com.example.remoteapp.schedule.ScheduleEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO sự kiện lịch cá nhân.
 *
 * @param id          id sự kiện
 * @param title       tiêu đề
 * @param description mô tả
 * @param startTime   bắt đầu
 * @param endTime     kết thúc
 * @param taskId      task liên kết
 * @param createdAt   thời điểm tạo
 * @reference docs/plan-backend/02-danh-sach-api.md §6
 */
public record ScheduleEventResponse(UUID id, String title, String description, Instant startTime,
                                    Instant endTime, UUID taskId, Instant createdAt) {

    /**
     * Ánh xạ entity sang DTO.
     *
     * @param event entity sự kiện
     * @return DTO phản hồi
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1 (mapping thủ công)
     */
    public static ScheduleEventResponse from(ScheduleEvent event) {
        return new ScheduleEventResponse(event.getId(), event.getTitle(), event.getDescription(),
                event.getStartTime(), event.getEndTime(), event.getTaskId(), event.getCreatedAt());
    }
}
