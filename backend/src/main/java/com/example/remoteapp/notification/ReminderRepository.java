package com.example.remoteapp.notification;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Query đọc task/assignee cho scheduler nhắc deadline.
 *
 * <p>Đây là query đọc tổng hợp phục vụ reminder (tương tự ngoại lệ của module stats,
 * plan 01 §4.2) — module notification không sửa dữ liệu của task.</p>
 *
 * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.2
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1
 */
@Repository
public class ReminderRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Khởi tạo repository (constructor injection tường minh).
     *
     * @param jdbcTemplate công cụ query JDBC
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public ReminderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Task có deadline trong khoảng [from, to), chưa DONE.
     *
     * @param from từ thời điểm
     * @param to   đến thời điểm (không tính)
     * @return danh sách task kèm người nhận
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1
     */
    public List<ReminderTask> findDueBetween(Instant from, Instant to) {
        return query("""
                select t.id, t.title, t.deadline, t.created_by, ta.user_id
                from tasks t
                left join task_assignees ta on ta.task_id = t.id
                where t.deleted_at is null and t.status <> 'DONE'
                  and t.deadline >= ? and t.deadline < ?
                """, from, to);
    }

    /**
     * Task đã quá hạn (deadline < now) và chưa DONE.
     *
     * @param now mốc thời gian hiện tại
     * @return danh sách task kèm người nhận
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1
     */
    public List<ReminderTask> findOverdue(Instant now) {
        return query("""
                select t.id, t.title, t.deadline, t.created_by, ta.user_id
                from tasks t
                left join task_assignees ta on ta.task_id = t.id
                where t.deleted_at is null and t.status <> 'DONE'
                  and t.deadline is not null and t.deadline < ?
                """, now, null);
    }

    /**
     * Chạy query và gộp assignee theo task (fallback người tạo khi không có assignee).
     *
     * @param sql      câu SQL (2 tham số: from, to — to null sẽ bỏ điều kiện)
     * @param from     mốc thứ nhất
     * @param to       mốc thứ hai (null nếu query chỉ dùng 1 mốc)
     * @return danh sách task
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1
     */
    private List<ReminderTask> query(String sql, Instant from, Instant to) {
        Map<UUID, ReminderTask> tasks = new LinkedHashMap<>();
        Object[] args = to == null
                ? new Object[] {java.sql.Timestamp.from(from)}
                : new Object[] {java.sql.Timestamp.from(from), java.sql.Timestamp.from(to)};
        jdbcTemplate.query(sql, rs -> {
            UUID taskId = rs.getObject("id", UUID.class);
            ReminderTask task = tasks.get(taskId);
            if (task == null) {
                task = new ReminderTask(taskId, rs.getString("title"),
                        rs.getTimestamp("deadline").toInstant(),
                        new LinkedHashSet<>(), rs.getObject("created_by", UUID.class));
                tasks.put(taskId, task);
            }
            UUID assigneeId = rs.getObject("user_id", UUID.class);
            if (assigneeId != null) {
                task.assigneeIds().add(assigneeId);
            }
        }, args);
        return List.copyOf(tasks.values());
    }

    /**
     * Task cần nhắc kèm danh sách người nhận.
     *
     * @param id          id task
     * @param title       tiêu đề
     * @param deadline    hạn hoàn thành
     * @param assigneeIds người được gán (rỗng = nhắc người tạo)
     * @param createdBy   người tạo
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1
     */
    public record ReminderTask(UUID id, String title, Instant deadline, Set<UUID> assigneeIds,
                               UUID createdBy) {
    }
}
