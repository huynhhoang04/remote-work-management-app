package com.example.remoteapp.stats.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Sự kiện rút gọn cho màn "Hôm nay".
 *
 * @param id        id sự kiện
 * @param title     tiêu đề
 * @param startTime bắt đầu
 * @param endTime   kết thúc
 * @param taskId    task liên kết
 * @reference docs/plan-backend/02-danh-sach-api.md §7
 */
public record EventBriefResponse(UUID id, String title, Instant startTime, Instant endTime,
                                 UUID taskId) {
}
