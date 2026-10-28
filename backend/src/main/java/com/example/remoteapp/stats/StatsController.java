package com.example.remoteapp.stats;

import com.example.remoteapp.common.SecurityUtils;
import com.example.remoteapp.stats.dto.MyStatsResponse;
import com.example.remoteapp.stats.dto.ProjectStatsResponse;
import com.example.remoteapp.stats.dto.TodayResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller thống kê (plan 02 §7).
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §7
 */
@RestController
@RequestMapping("/api/v1")
public class StatsController {

    private final StatsService statsService;

    /**
     * Khởi tạo controller (constructor injection tường minh).
     *
     * @param statsService service thống kê
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * Màn "Hôm nay" của người dùng.
     *
     * @return 4 nhóm dữ liệu
     * @reference docs/plan-backend/02-danh-sach-api.md §7
     */
    @GetMapping("/me/today")
    public ResponseEntity<TodayResponse> today() {
        return ResponseEntity.ok(statsService.getToday(SecurityUtils.currentUserId()));
    }

    /**
     * Thống kê năng suất cá nhân.
     *
     * @param from từ thời điểm (ISO-8601, tuỳ chọn)
     * @param to   đến thời điểm (ISO-8601, tuỳ chọn)
     * @return số task hoàn thành theo kỳ + tỉ lệ đúng hạn
     * @reference docs/plan-backend/02-danh-sach-api.md §7
     */
    @GetMapping("/me/stats")
    public ResponseEntity<MyStatsResponse> myStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(statsService.getMyStats(SecurityUtils.currentUserId(), from, to));
    }

    /**
     * Dashboard tiến độ dự án.
     *
     * @param id id dự án
     * @return thống kê dự án
     * @reference docs/plan-backend/02-danh-sach-api.md §7
     */
    @GetMapping("/projects/{id}/stats")
    public ResponseEntity<ProjectStatsResponse> projectStats(@PathVariable UUID id) {
        return ResponseEntity.ok(statsService.getProjectStats(id, SecurityUtils.currentUserId()));
    }
}
