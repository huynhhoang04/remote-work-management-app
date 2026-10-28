package com.example.remoteapp.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.schedule.dto.ScheduleEventRequest;
import com.example.remoteapp.schedule.dto.ScheduleEventResponse;
import com.example.remoteapp.task.TaskService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test cho {@link ScheduleService} — quyền sở hữu và khoảng thời gian.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1
 */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock private ScheduleEventRepository eventRepository;
    @Mock private TaskService taskService;

    private ScheduleService scheduleService;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(eventRepository, taskService, "Asia/Ho_Chi_Minh");
        actorId = UUID.randomUUID();
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1 */
    @Test
    void create_rejectsEndBeforeStart() {
        Instant start = Instant.parse("2026-11-01T09:00:00Z");
        ScheduleEventRequest request = new ScheduleEventRequest("Hop", null, start, start, null);

        assertThatThrownBy(() -> scheduleService.create(actorId, request))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(ErrorCode.SCHEDULE_TIME_INVALID));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1 */
    @Test
    void update_rejectsEventOfAnotherUser() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findByIdAndUserId(eventId, actorId)).thenReturn(Optional.empty());
        ScheduleEventRequest request = new ScheduleEventRequest("Hop", null,
                Instant.parse("2026-11-01T09:00:00Z"), Instant.parse("2026-11-01T10:00:00Z"), null);

        assertThatThrownBy(() -> scheduleService.update(eventId, actorId, request))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(ErrorCode.SCHEDULE_EVENT_NOT_FOUND));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1 */
    @Test
    void create_validatesLinkedTaskAccess() {
        UUID taskId = UUID.randomUUID();
        ScheduleEventRequest request = new ScheduleEventRequest("Hop", null,
                Instant.parse("2026-11-01T09:00:00Z"), Instant.parse("2026-11-01T10:00:00Z"), taskId);
        when(eventRepository.save(any(ScheduleEvent.class))).thenAnswer(invocation -> {
            ScheduleEvent saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ScheduleEventResponse response = scheduleService.create(actorId, request);

        assertThat(response.taskId()).isEqualTo(taskId);
        verify(taskService).requireAccessible(taskId, actorId);
    }
}
