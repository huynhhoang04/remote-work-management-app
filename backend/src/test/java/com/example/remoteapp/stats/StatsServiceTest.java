package com.example.remoteapp.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.project.ProjectPermissionService;
import com.example.remoteapp.stats.dto.EventBriefResponse;
import com.example.remoteapp.stats.dto.MyStatsResponse;
import com.example.remoteapp.stats.dto.ProjectStatsResponse;
import com.example.remoteapp.stats.dto.TaskBriefResponse;
import com.example.remoteapp.stats.dto.TodayResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test cho {@link StatsService} — công thức phần trăm, khoảng thời gian, "Hôm nay".
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D2, §D3
 */
@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock private StatsRepository statsRepository;
    @Mock private ProjectPermissionService permissionService;

    private StatsService statsService;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        statsService = new StatsService(statsRepository, permissionService, "Asia/Ho_Chi_Minh");
        actorId = UUID.randomUUID();
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D3 */
    @Test
    void projectStats_computesPercentDone() {
        UUID projectId = UUID.randomUUID();
        when(statsRepository.countProjectTasks(projectId)).thenReturn(4L);
        when(statsRepository.countProjectTasksByStatus(projectId))
                .thenReturn(Map.of("TODO", 3L, "DONE", 1L));
        when(statsRepository.countProjectOverdue(eq(projectId), any())).thenReturn(2L);
        when(statsRepository.projectMemberStats(projectId)).thenReturn(List.of(
                new ProjectStatsResponse.MemberStat(actorId, "Actor", 4, 1)));

        ProjectStatsResponse response = statsService.getProjectStats(projectId, actorId);

        assertThat(response.totalTasks()).isEqualTo(4);
        assertThat(response.percentDone()).isEqualTo(25.0);
        assertThat(response.overdueCount()).isEqualTo(2);
        verify(permissionService).requireMember(projectId, actorId);
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D3 */
    @Test
    void myStats_rejectsInvalidRange() {
        Instant from = Instant.parse("2026-11-02T00:00:00Z");
        Instant to = Instant.parse("2026-11-01T00:00:00Z");

        assertThatThrownBy(() -> statsService.getMyStats(actorId, from, to))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STATS_RANGE_INVALID));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D2 */
    @Test
    void today_groupsTasksAndEvents() {
        UUID projectId = UUID.randomUUID();
        TaskBriefResponse overdue = new TaskBriefResponse(UUID.randomUUID(), projectId, "Qua han",
                "TODO", "HIGH", Instant.now().minusSeconds(3600));
        EventBriefResponse event = new EventBriefResponse(UUID.randomUUID(), "Hop",
                Instant.now(), Instant.now().plusSeconds(3600), null);
        when(statsRepository.findAssignedDueBefore(eq(actorId), any())).thenReturn(List.of(overdue));
        when(statsRepository.findAssignedDueBetween(eq(actorId), any(), any()))
                .thenReturn(List.of(overdue))
                .thenReturn(List.of());
        when(statsRepository.findEventsBetween(eq(actorId), any(), any())).thenReturn(List.of(event));

        TodayResponse response = statsService.getToday(actorId);

        assertThat(response.overdueTasks()).containsExactly(overdue);
        assertThat(response.dueTodayTasks()).containsExactly(overdue);
        assertThat(response.upcomingTasks()).isEmpty();
        assertThat(response.todayEvents()).containsExactly(event);
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D3 */
    @Test
    void myStats_returnsSeriesAndOnTimeRate() {
        when(statsRepository.completedPerDay(eq(actorId), any(), any(), eq("Asia/Ho_Chi_Minh")))
                .thenReturn(List.of(new MyStatsResponse.CountPoint("2026-10-28", 2)));
        when(statsRepository.completedPerWeek(eq(actorId), any(), any(), eq("Asia/Ho_Chi_Minh")))
                .thenReturn(List.of(new MyStatsResponse.CountPoint("2026-W44", 2)));
        when(statsRepository.completedPerMonth(eq(actorId), any(), any(), eq("Asia/Ho_Chi_Minh")))
                .thenReturn(List.of(new MyStatsResponse.CountPoint("2026-10", 2)));
        when(statsRepository.onTimeRate(eq(actorId), any(), any())).thenReturn(0.75);

        MyStatsResponse response = statsService.getMyStats(actorId, null, null);

        assertThat(response.completedPerDay()).hasSize(1);
        assertThat(response.completedPerWeek()).hasSize(1);
        assertThat(response.completedPerMonth()).hasSize(1);
        assertThat(response.onTimeRate()).isEqualTo(0.75);
    }
}
