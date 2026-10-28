package com.example.remoteapp.stats;

import com.example.remoteapp.stats.dto.EventBriefResponse;
import com.example.remoteapp.stats.dto.MyStatsResponse;
import com.example.remoteapp.stats.dto.ProjectStatsResponse;
import com.example.remoteapp.stats.dto.TaskBriefResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Query đọc tổng hợp cho module stats (ngoại lệ được phép trong plan 01 §4.2:
 * stats chỉ đọc dữ liệu của task/project, không sửa).
 *
 * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.2
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D2, §D3
 */
@Repository
public class StatsRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Khởi tạo repository (constructor injection tường minh).
     *
     * @param jdbcTemplate công cụ query JDBC
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public StatsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Task được gán cho người dùng có deadline trước một mốc (quá hạn).
     *
     * @param userId người dùng
     * @param before mốc thời gian
     * @return danh sách task rút gọn
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D2
     */
    public List<TaskBriefResponse> findAssignedDueBefore(UUID userId, Instant before) {
        return jdbcTemplate.query("""
                select distinct t.id, t.project_id, t.title, t.status, t.priority, t.deadline
                from tasks t
                join task_assignees ta on ta.task_id = t.id
                where ta.user_id = ? and t.deleted_at is null and t.status <> 'DONE'
                  and t.deadline is not null and t.deadline < ?
                order by t.deadline
                """, (rs, rowNum) -> mapTask(rs), userId, Timestamp.from(before));
    }

    /**
     * Task được gán cho người dùng có deadline trong khoảng.
     *
     * @param userId người dùng
     * @param from   từ thời điểm
     * @param to     đến thời điểm
     * @return danh sách task rút gọn
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D2
     */
    public List<TaskBriefResponse> findAssignedDueBetween(UUID userId, Instant from, Instant to) {
        return jdbcTemplate.query("""
                select distinct t.id, t.project_id, t.title, t.status, t.priority, t.deadline
                from tasks t
                join task_assignees ta on ta.task_id = t.id
                where ta.user_id = ? and t.deleted_at is null and t.status <> 'DONE'
                  and t.deadline is not null and t.deadline >= ? and t.deadline < ?
                order by t.deadline
                """, (rs, rowNum) -> mapTask(rs), userId, Timestamp.from(from), Timestamp.from(to));
    }

    /**
     * Sự kiện lịch của người dùng trong khoảng.
     *
     * @param userId người dùng
     * @param from   từ thời điểm
     * @param to     đến thời điểm
     * @return danh sách sự kiện rút gọn
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D2
     */
    public List<EventBriefResponse> findEventsBetween(UUID userId, Instant from, Instant to) {
        return jdbcTemplate.query("""
                select id, title, start_time, end_time, task_id
                from schedule_events
                where user_id = ? and start_time >= ? and start_time < ?
                order by start_time
                """, (rs, rowNum) -> new EventBriefResponse(
                        rs.getObject("id", UUID.class),
                        rs.getString("title"),
                        rs.getTimestamp("start_time").toInstant(),
                        rs.getTimestamp("end_time").toInstant(),
                        rs.getObject("task_id", UUID.class)),
                userId, Timestamp.from(from), Timestamp.from(to));
    }

    /**
     * Số task hoàn thành theo ngày.
     *
     * @param userId người dùng
     * @param from   từ thời điểm
     * @param to     đến thời điểm
     * @param zone   múi giờ nghiệp vụ
     * @return danh sách điểm dữ liệu
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D3
     */
    public List<MyStatsResponse.CountPoint> completedPerDay(UUID userId, Instant from, Instant to,
                                                            String zone) {
        return jdbcTemplate.query("""
                select to_char((t.completed_at at time zone ?)::date, 'YYYY-MM-DD') as period,
                       count(*) as total
                from tasks t
                join task_assignees ta on ta.task_id = t.id
                where ta.user_id = ? and t.status = 'DONE' and t.deleted_at is null
                  and t.completed_at >= ? and t.completed_at < ?
                group by period order by period
                """, (rs, rowNum) -> new MyStatsResponse.CountPoint(
                        rs.getString("period"), rs.getLong("total")),
                zone, userId, Timestamp.from(from), Timestamp.from(to));
    }

    /**
     * Số task hoàn thành theo tuần (ISO week).
     *
     * @param userId người dùng
     * @param from   từ thời điểm
     * @param to     đến thời điểm
     * @param zone   múi giờ nghiệp vụ
     * @return danh sách điểm dữ liệu
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D3
     */
    public List<MyStatsResponse.CountPoint> completedPerWeek(UUID userId, Instant from, Instant to,
                                                             String zone) {
        return jdbcTemplate.query("""
                select to_char(date_trunc('week', t.completed_at at time zone ?),
                               'IYYY-"W"IW') as period,
                       count(*) as total
                from tasks t
                join task_assignees ta on ta.task_id = t.id
                where ta.user_id = ? and t.status = 'DONE' and t.deleted_at is null
                  and t.completed_at >= ? and t.completed_at < ?
                group by period order by period
                """, (rs, rowNum) -> new MyStatsResponse.CountPoint(
                        rs.getString("period"), rs.getLong("total")),
                zone, userId, Timestamp.from(from), Timestamp.from(to));
    }

    /**
     * Số task hoàn thành theo tháng.
     *
     * @param userId người dùng
     * @param from   từ thời điểm
     * @param to     đến thời điểm
     * @param zone   múi giờ nghiệp vụ
     * @return danh sách điểm dữ liệu
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D3
     */
    public List<MyStatsResponse.CountPoint> completedPerMonth(UUID userId, Instant from, Instant to,
                                                              String zone) {
        return jdbcTemplate.query("""
                select to_char(date_trunc('month', t.completed_at at time zone ?), 'YYYY-MM') as period,
                       count(*) as total
                from tasks t
                join task_assignees ta on ta.task_id = t.id
                where ta.user_id = ? and t.status = 'DONE' and t.deleted_at is null
                  and t.completed_at >= ? and t.completed_at < ?
                group by period order by period
                """, (rs, rowNum) -> new MyStatsResponse.CountPoint(
                        rs.getString("period"), rs.getLong("total")),
                zone, userId, Timestamp.from(from), Timestamp.from(to));
    }

    /**
     * Tỉ lệ task hoàn thành đúng hạn trong khoảng.
     *
     * @param userId người dùng
     * @param from   từ thời điểm
     * @param to     đến thời điểm
     * @return tỉ lệ 0–1 (0 nếu không có task có deadline)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D3
     */
    public double onTimeRate(UUID userId, Instant from, Instant to) {
        Double rate = jdbcTemplate.queryForObject("""
                select case when count(*) filter (where t.deadline is not null) = 0 then 0
                            else count(*) filter (where t.deadline is not null
                                                  and t.completed_at <= t.deadline)::numeric
                                 / count(*) filter (where t.deadline is not null)
                       end
                from tasks t
                join task_assignees ta on ta.task_id = t.id
                where ta.user_id = ? and t.status = 'DONE' and t.deleted_at is null
                  and t.completed_at >= ? and t.completed_at < ?
                """, Double.class, userId, Timestamp.from(from), Timestamp.from(to));
        return rate == null ? 0 : rate;
    }

    /**
     * Tổng số task chưa xoá của dự án.
     *
     * @param projectId dự án
     * @return tổng số
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D3
     */
    public long countProjectTasks(UUID projectId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from tasks where project_id = ? and deleted_at is null",
                Long.class, projectId);
        return count == null ? 0 : count;
    }

    /**
     * Số task theo từng trạng thái trong dự án.
     *
     * @param projectId dự án
     * @return map status → số lượng
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D3
     */
    public Map<String, Long> countProjectTasksByStatus(UUID projectId) {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                select status, count(*) as total from tasks
                where project_id = ? and deleted_at is null
                group by status
                """, rs -> {
                    result.put(rs.getString("status"), rs.getLong("total"));
                }, projectId);
        return result;
    }

    /**
     * Số task quá hạn chưa xong của dự án.
     *
     * @param projectId dự án
     * @param now       mốc thời gian
     * @return số task quá hạn
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D3
     */
    public long countProjectOverdue(UUID projectId, Instant now) {
        Long count = jdbcTemplate.queryForObject("""
                select count(*) from tasks
                where project_id = ? and deleted_at is null and status <> 'DONE'
                  and deadline is not null and deadline < ?
                """, Long.class, projectId, Timestamp.from(now));
        return count == null ? 0 : count;
    }

    /**
     * Thống kê task theo từng thành viên dự án.
     *
     * @param projectId dự án
     * @return danh sách thống kê thành viên
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D3
     */
    public List<ProjectStatsResponse.MemberStat> projectMemberStats(UUID projectId) {
        return jdbcTemplate.query("""
                select pm.user_id, u.full_name,
                       count(distinct t.id) as total,
                       count(distinct case when t.status = 'DONE' then t.id end) as done
                from project_members pm
                join users u on u.id = pm.user_id
                left join task_assignees ta on ta.user_id = pm.user_id
                left join tasks t on t.id = ta.task_id
                     and t.project_id = ? and t.deleted_at is null
                where pm.project_id = ?
                group by pm.user_id, u.full_name
                order by u.full_name
                """, (rs, rowNum) -> new ProjectStatsResponse.MemberStat(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("full_name"),
                        rs.getLong("total"),
                        rs.getLong("done")),
                projectId, projectId);
    }

    /**
     * Ánh xạ dòng task rút gọn.
     *
     * @param rs result set
     * @return DTO task rút gọn
     * @throws SQLException lỗi đọc cột
     * @reference docs/plan-backend/02-danh-sach-api.md §7
     */
    private TaskBriefResponse mapTask(ResultSet rs) throws SQLException {
        Timestamp deadline = rs.getTimestamp("deadline");
        return new TaskBriefResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("project_id", UUID.class),
                rs.getString("title"),
                rs.getString("status"),
                rs.getString("priority"),
                deadline == null ? null : deadline.toInstant());
    }
}
