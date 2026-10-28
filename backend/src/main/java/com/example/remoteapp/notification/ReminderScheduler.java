package com.example.remoteapp.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Quét task tới hạn/quá hạn mỗi 5 phút và tạo thông báo (chống trùng bằng
 * {@code dedupe_key}), gửi push qua {@link NotificationService} (plan 03 §E1).
 *
 * <p>Mốc cố định: trước 1 ngày, trước 1 giờ, quá hạn (1 lần/ngày). Task không có
 * assignee thì nhắc người tạo; task không có deadline bị bỏ qua.</p>
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1
 */
@Slf4j
@Component
public class ReminderScheduler {

    private static final String REF_TYPE = "TASK";

    private final ReminderRepository reminderRepository;
    private final NotificationService notificationService;
    private final long intervalMs;
    private final ZoneId zone;

    /**
     * Khởi tạo scheduler (constructor injection tường minh).
     *
     * @param reminderRepository  query task tới hạn
     * @param notificationService tạo thông báo + push
     * @param intervalMs          chu kỳ quét (cũng là độ rộng cửa sổ nhắc)
     * @param timezone            múi giờ nghiệp vụ (dùng cho khoá quá hạn theo ngày)
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public ReminderScheduler(ReminderRepository reminderRepository,
                             NotificationService notificationService,
                             @Value("${app.reminder.interval-ms:300000}") long intervalMs,
                             @Value("${app.timezone:Asia/Ho_Chi_Minh}") String timezone) {
        this.reminderRepository = reminderRepository;
        this.notificationService = notificationService;
        this.intervalMs = intervalMs;
        this.zone = ZoneId.of(timezone);
    }

    /**
     * Quét và gửi nhắc deadline định kỳ.
     *
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1
     */
    @Scheduled(fixedDelayString = "${app.reminder.interval-ms:300000}",
            initialDelayString = "${app.reminder.initial-delay-ms:60000}")
    public void scan() {
        Instant now = Instant.now();
        try {
            remindWindow(now.plus(1, ChronoUnit.DAYS),
                    now.plus(1, ChronoUnit.DAYS).plusMillis(intervalMs),
                    NotificationType.REMIND_1D,
                    "Task sắp đến hạn (còn 1 ngày)");
            remindWindow(now.plus(1, ChronoUnit.HOURS),
                    now.plus(1, ChronoUnit.HOURS).plusMillis(intervalMs),
                    NotificationType.REMIND_1H,
                    "Task sắp đến hạn (còn 1 giờ)");
            String today = LocalDate.now(zone).toString();
            for (ReminderRepository.ReminderTask task : reminderRepository.findOverdue(now)) {
                for (UUID userId : recipients(task)) {
                    notificationService.notify(userId, NotificationType.OVERDUE,
                            "Task đã quá hạn", task.title(), REF_TYPE, task.id(),
                            "OVERDUE:" + task.id() + ":" + userId + ":" + today);
                }
            }
        } catch (RuntimeException ex) {
            log.error("Lỗi khi quét nhắc deadline", ex);
        }
    }

    /**
     * Nhắc các task rơi vào một cửa sổ thời gian.
     *
     * @param from    bắt đầu cửa sổ
     * @param to      kết thúc cửa sổ
     * @param type    loại nhắc
     * @param title   tiêu đề thông báo
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1
     */
    private void remindWindow(Instant from, Instant to, NotificationType type, String title) {
        for (ReminderRepository.ReminderTask task : reminderRepository.findDueBetween(from, to)) {
            for (UUID userId : recipients(task)) {
                notificationService.notify(userId, type, title, task.title(), REF_TYPE, task.id(),
                        type.name() + ":" + task.id() + ":" + userId);
            }
        }
    }

    /**
     * Người nhận nhắc: assignee, nếu không có thì người tạo task.
     *
     * @param task task cần nhắc
     * @return tập người nhận
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1
     */
    private Set<UUID> recipients(ReminderRepository.ReminderTask task) {
        if (!task.assigneeIds().isEmpty()) {
            return task.assigneeIds();
        }
        return task.createdBy() == null ? Set.of() : Set.of(task.createdBy());
    }
}
