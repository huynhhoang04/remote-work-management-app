package com.example.remoteapp.stats.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Thông tin rút gọn của task cho màn "Hôm nay".
 *
 * @param id        id task
 * @param projectId dự án
 * @param title     tiêu đề
 * @param status    trạng thái
 * @param priority  độ ưu tiên
 * @param deadline  hạn hoàn thành
 * @reference docs/plan-backend/02-danh-sach-api.md §7
 */
public record TaskBriefResponse(UUID id, UUID projectId, String title, String status,
                                String priority, Instant deadline) {
}
