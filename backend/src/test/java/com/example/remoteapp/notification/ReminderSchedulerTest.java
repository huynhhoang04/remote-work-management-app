package com.example.remoteapp.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test cho {@link ReminderScheduler} — đủ 3 mốc, dedupe key, fallback người tạo.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1
 */
@ExtendWith(MockitoExtension.class)
class ReminderSchedulerTest {

    @Mock private ReminderRepository reminderRepository;
    @Mock private NotificationService notificationService;

    private ReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ReminderScheduler(reminderRepository, notificationService, 300_000,
                "Asia/Ho_Chi_Minh");
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1 */
    @Test
    void scan_sendsAllThreeMilestonesForAssignees() {
        UUID taskId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        ReminderRepository.ReminderTask task = new ReminderRepository.ReminderTask(taskId, "Nộp báo cáo",
                Instant.now().plusSeconds(3600), Set.of(assigneeId), UUID.randomUUID());
        when(reminderRepository.findDueBetween(any(), any())).thenReturn(List.of(task));
        when(reminderRepository.findOverdue(any())).thenReturn(List.of(task));

        scheduler.scan();

        verify(notificationService).notify(eq(assigneeId), eq(NotificationType.REMIND_1D),
                anyString(), eq("Nộp báo cáo"), eq("TASK"), eq(taskId),
                startsWith("REMIND_1D:" + taskId + ":" + assigneeId));
        verify(notificationService).notify(eq(assigneeId), eq(NotificationType.REMIND_1H),
                anyString(), eq("Nộp báo cáo"), eq("TASK"), eq(taskId),
                startsWith("REMIND_1H:" + taskId + ":" + assigneeId));
        verify(notificationService).notify(eq(assigneeId), eq(NotificationType.OVERDUE),
                anyString(), eq("Nộp báo cáo"), eq("TASK"), eq(taskId),
                startsWith("OVERDUE:" + taskId + ":" + assigneeId + ":"));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1 */
    @Test
    void scan_fallsBackToCreatorWhenNoAssignee() {
        UUID taskId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        ReminderRepository.ReminderTask task = new ReminderRepository.ReminderTask(taskId, "Task",
                Instant.now().minusSeconds(3600), Set.of(), creatorId);
        when(reminderRepository.findDueBetween(any(), any())).thenReturn(List.of());
        when(reminderRepository.findOverdue(any())).thenReturn(List.of(task));

        scheduler.scan();

        verify(notificationService).notify(eq(creatorId), eq(NotificationType.OVERDUE),
                anyString(), eq("Task"), eq("TASK"), eq(taskId),
                startsWith("OVERDUE:" + taskId + ":" + creatorId + ":"));
    }
}
