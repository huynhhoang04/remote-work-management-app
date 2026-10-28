package com.example.remoteapp.stats.dto;

import java.util.List;

/**
 * Dữ liệu màn "Hôm nay" — gộp 4 nhóm trong 1 call (FR-C02).
 *
 * @param overdueTasks  task quá hạn chưa xong
 * @param dueTodayTasks task đến hạn hôm nay
 * @param upcomingTasks task đến hạn trong 7 ngày tới
 * @param todayEvents   sự kiện lịch hôm nay
 * @reference docs/plan-backend/02-danh-sach-api.md §7
 */
public record TodayResponse(List<TaskBriefResponse> overdueTasks,
                            List<TaskBriefResponse> dueTodayTasks,
                            List<TaskBriefResponse> upcomingTasks,
                            List<EventBriefResponse> todayEvents) {
}
