package com.example.remoteapp.stats.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dashboard tiến độ dự án (FR-C01, FR-C05).
 *
 * @param totalTasks   tổng số task
 * @param perStatus    số task theo từng trạng thái
 * @param percentDone  phần trăm hoàn thành (0–100)
 * @param overdueCount số task quá hạn chưa xong
 * @param perMember    thống kê theo thành viên
 * @reference docs/plan-backend/02-danh-sach-api.md §7
 */
public record ProjectStatsResponse(long totalTasks, Map<String, Long> perStatus, double percentDone,
                                   long overdueCount, List<MemberStat> perMember) {

    /**
     * Thống kê task của một thành viên.
     *
     * @param userId   id thành viên
     * @param fullName họ tên
     * @param total    số task được gán
     * @param done     số task đã xong
     * @reference docs/plan-backend/02-danh-sach-api.md §7
     */
    public record MemberStat(UUID userId, String fullName, long total, long done) {
    }
}
