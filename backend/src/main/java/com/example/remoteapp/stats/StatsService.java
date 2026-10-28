package com.example.remoteapp.stats;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.project.ProjectPermissionService;
import com.example.remoteapp.stats.dto.MyStatsResponse;
import com.example.remoteapp.stats.dto.ProjectStatsResponse;
import com.example.remoteapp.stats.dto.TodayResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ thống kê: "Hôm nay", năng suất cá nhân, dashboard dự án (plan 03 §D2, §D3).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D2, §D3
 */
@Service
public class StatsService {

    private static final int UPCOMING_DAYS = 7;
    private static final int DEFAULT_STATS_DAYS = 30;

    private final StatsRepository statsRepository;
    private final ProjectPermissionService permissionService;
    private final ZoneId zone;

    /**
     * Khởi tạo service (constructor injection tường minh).
     *
     * @param statsRepository   query tổng hợp
     * @param permissionService kiểm tra quyền dự án
     * @param timezone          múi giờ nghiệp vụ (app.timezone)
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public StatsService(StatsRepository statsRepository, ProjectPermissionService permissionService,
                        @Value("${app.timezone:Asia/Ho_Chi_Minh}") String timezone) {
        this.statsRepository = statsRepository;
        this.permissionService = permissionService;
        this.zone = ZoneId.of(timezone);
    }

    /**
     * Màn "Hôm nay": quá hạn, đến hạn hôm nay, 7 ngày tới, sự kiện hôm nay.
     *
     * @param actorId người dùng
     * @return dữ liệu 4 nhóm
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D2
     */
    @Transactional(readOnly = true)
    public TodayResponse getToday(UUID actorId) {
        LocalDate today = LocalDate.now(zone);
        Instant startOfToday = today.atStartOfDay(zone).toInstant();
        Instant startOfTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant upcomingEnd = today.plusDays(1 + UPCOMING_DAYS).atStartOfDay(zone).toInstant();

        return new TodayResponse(
                statsRepository.findAssignedDueBefore(actorId, startOfToday),
                statsRepository.findAssignedDueBetween(actorId, startOfToday, startOfTomorrow),
                statsRepository.findAssignedDueBetween(actorId, startOfTomorrow, upcomingEnd),
                statsRepository.findEventsBetween(actorId, startOfToday, startOfTomorrow));
    }

    /**
     * Thống kê năng suất cá nhân (mặc định 30 ngày gần nhất).
     *
     * @param actorId người dùng
     * @param from    từ thời điểm (null = 30 ngày trước)
     * @param to      đến thời điểm (null = bây giờ)
     * @return số task hoàn thành theo ngày/tuần/tháng + tỉ lệ đúng hạn
     * @throws AppException STATS_RANGE_INVALID nếu from > to
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D3
     */
    @Transactional(readOnly = true)
    public MyStatsResponse getMyStats(UUID actorId, Instant from, Instant to) {
        Instant end = to != null ? to : Instant.now();
        Instant start = from != null ? from : end.minus(DEFAULT_STATS_DAYS, ChronoUnit.DAYS);
        if (start.isAfter(end)) {
            throw new AppException(ErrorCode.STATS_RANGE_INVALID, "Khoảng thời gian không hợp lệ");
        }
        String zoneId = zone.getId();
        List<MyStatsResponse.CountPoint> perDay =
                statsRepository.completedPerDay(actorId, start, end, zoneId);
        List<MyStatsResponse.CountPoint> perWeek =
                statsRepository.completedPerWeek(actorId, start, end, zoneId);
        List<MyStatsResponse.CountPoint> perMonth =
                statsRepository.completedPerMonth(actorId, start, end, zoneId);
        return new MyStatsResponse(perDay, perWeek, perMonth,
                statsRepository.onTimeRate(actorId, start, end));
    }

    /**
     * Dashboard tiến độ dự án (mọi thành viên, kể cả VIEWER).
     *
     * @param projectId dự án
     * @param actorId   người yêu cầu
     * @return thống kê dự án
     * @throws AppException PROJECT_NOT_FOUND / PROJECT_FORBIDDEN
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D3
     */
    @Transactional(readOnly = true)
    public ProjectStatsResponse getProjectStats(UUID projectId, UUID actorId) {
        permissionService.requireMember(projectId, actorId);
        long total = statsRepository.countProjectTasks(projectId);
        Map<String, Long> perStatus = statsRepository.countProjectTasksByStatus(projectId);
        long done = perStatus.getOrDefault("DONE", 0L);
        double percentDone = total == 0 ? 0 : Math.round(done * 10000.0 / total) / 100.0;
        return new ProjectStatsResponse(total, perStatus, percentDone,
                statsRepository.countProjectOverdue(projectId, Instant.now()),
                statsRepository.projectMemberStats(projectId));
    }
}
