package com.example.remoteapp.stats.dto;

import java.util.List;

/**
 * Thống kê năng suất cá nhân (FR-C04).
 *
 * @param completedPerDay   số task hoàn thành theo ngày
 * @param completedPerWeek  số task hoàn thành theo tuần
 * @param completedPerMonth số task hoàn thành theo tháng
 * @param onTimeRate        tỉ lệ hoàn thành đúng hạn (0–1)
 * @reference docs/plan-backend/02-danh-sach-api.md §7
 */
public record MyStatsResponse(List<CountPoint> completedPerDay, List<CountPoint> completedPerWeek,
                              List<CountPoint> completedPerMonth, double onTimeRate) {

    /**
     * Một điểm dữ liệu (kỳ → số lượng).
     *
     * @param period kỳ (YYYY-MM-DD / YYYY-Www / YYYY-MM)
     * @param count  số task
     * @reference docs/plan-backend/02-danh-sach-api.md §7
     */
    public record CountPoint(String period, long count) {
    }
}
