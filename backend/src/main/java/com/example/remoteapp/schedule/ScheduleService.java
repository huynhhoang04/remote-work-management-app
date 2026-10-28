package com.example.remoteapp.schedule;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.schedule.dto.ScheduleEventRequest;
import com.example.remoteapp.schedule.dto.ScheduleEventResponse;
import com.example.remoteapp.task.TaskService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ lịch làm việc cá nhân (plan 03 §D1).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1
 */
@Service
public class ScheduleService {

    private static final int DEFAULT_RANGE_DAYS = 7;

    private final ScheduleEventRepository eventRepository;
    private final TaskService taskService;
    private final ZoneId zone;

    /**
     * Khởi tạo service (constructor injection tường minh).
     *
     * @param eventRepository repository sự kiện
     * @param taskService     kiểm tra task liên kết
     * @param timezone        múi giờ nghiệp vụ (app.timezone)
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public ScheduleService(ScheduleEventRepository eventRepository, TaskService taskService,
                           @Value("${app.timezone:Asia/Ho_Chi_Minh}") String timezone) {
        this.eventRepository = eventRepository;
        this.taskService = taskService;
        this.zone = ZoneId.of(timezone);
    }

    /**
     * Sự kiện trong khoảng (mặc định hôm nay → +7 ngày).
     *
     * @param actorId người yêu cầu
     * @param from    từ thời điểm (null = đầu hôm nay)
     * @param to      đến thời điểm (null = +7 ngày)
     * @return danh sách sự kiện
     * @throws AppException SCHEDULE_TIME_INVALID nếu from > to
     * @reference docs/plan-backend/02-danh-sach-api.md §6
     */
    @Transactional(readOnly = true)
    public List<ScheduleEventResponse> list(UUID actorId, Instant from, Instant to) {
        Instant start = from != null ? from : LocalDate.now(zone).atStartOfDay(zone).toInstant();
        Instant end = to != null ? to : LocalDate.now(zone).plusDays(DEFAULT_RANGE_DAYS)
                .atStartOfDay(zone).toInstant();
        if (start.isAfter(end)) {
            throw new AppException(ErrorCode.SCHEDULE_TIME_INVALID,
                    "Khoảng thời gian không hợp lệ");
        }
        return eventRepository.findByUserIdAndStartTimeBetweenOrderByStartTimeAsc(actorId, start, end)
                .stream()
                .map(ScheduleEventResponse::from)
                .toList();
    }

    /**
     * Tạo sự kiện cá nhân.
     *
     * @param actorId người tạo
     * @param request dữ liệu sự kiện
     * @return sự kiện mới
     * @throws AppException SCHEDULE_TIME_INVALID nếu startTime ≥ endTime
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1
     */
    @Transactional
    public ScheduleEventResponse create(UUID actorId, ScheduleEventRequest request) {
        validateWindow(request.startTime(), request.endTime());
        if (request.taskId() != null) {
            taskService.requireAccessible(request.taskId(), actorId);
        }
        ScheduleEvent event = new ScheduleEvent();
        event.setUserId(actorId);
        apply(event, request);
        eventRepository.save(event);
        return ScheduleEventResponse.from(event);
    }

    /**
     * Sửa sự kiện (chỉ chủ sở hữu).
     *
     * @param eventId id sự kiện
     * @param actorId người yêu cầu
     * @param request dữ liệu mới
     * @return sự kiện sau cập nhật
     * @throws AppException SCHEDULE_EVENT_NOT_FOUND / SCHEDULE_TIME_INVALID
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1
     */
    @Transactional
    public ScheduleEventResponse update(UUID eventId, UUID actorId, ScheduleEventRequest request) {
        ScheduleEvent event = findOwned(eventId, actorId);
        validateWindow(request.startTime(), request.endTime());
        if (request.taskId() != null) {
            taskService.requireAccessible(request.taskId(), actorId);
        }
        apply(event, request);
        eventRepository.save(event);
        return ScheduleEventResponse.from(event);
    }

    /**
     * Xoá sự kiện (chỉ chủ sở hữu).
     *
     * @param eventId id sự kiện
     * @param actorId người yêu cầu
     * @throws AppException SCHEDULE_EVENT_NOT_FOUND
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1
     */
    @Transactional
    public void delete(UUID eventId, UUID actorId) {
        eventRepository.delete(findOwned(eventId, actorId));
    }

    /**
     * Áp dữ liệu request vào entity.
     *
     * @param event   entity sự kiện
     * @param request dữ liệu mới
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1
     */
    private void apply(ScheduleEvent event, ScheduleEventRequest request) {
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setTaskId(request.taskId());
    }

    /**
     * Validate startTime < endTime.
     *
     * @param startTime bắt đầu
     * @param endTime   kết thúc
     * @throws AppException SCHEDULE_TIME_INVALID
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1
     */
    private void validateWindow(Instant startTime, Instant endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new AppException(ErrorCode.SCHEDULE_TIME_INVALID,
                    "Thời điểm kết thúc phải sau thời điểm bắt đầu");
        }
    }

    /**
     * Tìm sự kiện của chính người dùng (không lộ sự kiện người khác).
     *
     * @param eventId id sự kiện
     * @param actorId người yêu cầu
     * @return entity sự kiện
     * @throws AppException SCHEDULE_EVENT_NOT_FOUND
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1
     */
    private ScheduleEvent findOwned(UUID eventId, UUID actorId) {
        return eventRepository.findByIdAndUserId(eventId, actorId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_EVENT_NOT_FOUND,
                        "Không tìm thấy sự kiện"));
    }
}
