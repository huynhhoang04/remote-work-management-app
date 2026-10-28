package com.example.remoteapp.schedule;

import com.example.remoteapp.common.SecurityUtils;
import com.example.remoteapp.schedule.dto.ScheduleEventRequest;
import com.example.remoteapp.schedule.dto.ScheduleEventResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller lịch làm việc cá nhân (plan 02 §6).
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §6
 */
@RestController
@RequestMapping("/api/v1")
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * Khởi tạo controller (constructor injection tường minh).
     *
     * @param scheduleService service lịch cá nhân
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * Danh sách sự kiện trong khoảng thời gian.
     *
     * @param from từ thời điểm (ISO-8601, tuỳ chọn)
     * @param to   đến thời điểm (ISO-8601, tuỳ chọn)
     * @return danh sách sự kiện
     * @reference docs/plan-backend/02-danh-sach-api.md §6
     */
    @GetMapping("/schedule/events")
    public ResponseEntity<List<ScheduleEventResponse>> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(scheduleService.list(SecurityUtils.currentUserId(), from, to));
    }

    /**
     * Tạo sự kiện cá nhân.
     *
     * @param request dữ liệu sự kiện
     * @return 201 + sự kiện
     * @reference docs/plan-backend/02-danh-sach-api.md §6
     */
    @PostMapping("/schedule/events")
    public ResponseEntity<ScheduleEventResponse> create(
            @Valid @RequestBody ScheduleEventRequest request) {
        ScheduleEventResponse response = scheduleService.create(SecurityUtils.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/v1/schedule/events/" + response.id()))
                .body(response);
    }

    /**
     * Sửa sự kiện.
     *
     * @param id      id sự kiện
     * @param request dữ liệu mới
     * @return sự kiện sau cập nhật
     * @reference docs/plan-backend/02-danh-sach-api.md §6
     */
    @PatchMapping("/schedule/events/{id}")
    public ResponseEntity<ScheduleEventResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody
                                                        ScheduleEventRequest request) {
        return ResponseEntity.ok(scheduleService.update(id, SecurityUtils.currentUserId(), request));
    }

    /**
     * Xoá sự kiện.
     *
     * @param id id sự kiện
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §6
     */
    @DeleteMapping("/schedule/events/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        scheduleService.delete(id, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
