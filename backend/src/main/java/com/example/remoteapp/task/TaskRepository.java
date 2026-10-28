package com.example.remoteapp.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository cho {@link Task}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
 */
public interface TaskRepository extends JpaRepository<Task, UUID> {

    /**
     * Tìm task chưa bị xoá.
     *
     * @param id id task
     * @return task nếu tồn tại
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    Optional<Task> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * Toàn bộ task (gốc + subtask) của dự án, sắp theo vị trí Kanban.
     *
     * @param projectId dự án
     * @return danh sách task
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    List<Task> findByProjectIdAndDeletedAtIsNullOrderByPositionAscCreatedAtAsc(UUID projectId);

    /**
     * Các subtask của một task gốc.
     *
     * @param parentTaskId task cha
     * @return danh sách subtask
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    List<Task> findByParentTaskIdAndDeletedAtIsNullOrderByPositionAscCreatedAtAsc(UUID parentTaskId);

    /**
     * Tất cả subtask (kể cả đã xoá) để dọn dẹp khi xoá task cha.
     *
     * @param parentTaskId task cha
     * @return danh sách subtask
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    List<Task> findByParentTaskId(UUID parentTaskId);

    /**
     * Vị trí lớn nhất trong một cột Kanban của dự án (task gốc).
     *
     * @param projectId dự án
     * @param status    trạng thái/cột
     * @return vị trí lớn nhất (0 nếu cột rỗng)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    @Query("select coalesce(max(t.position), 0) from Task t "
            + "where t.projectId = :projectId and t.status = :status "
            + "and t.deletedAt is null and t.parentTaskId is null")
    double maxRootPosition(@Param("projectId") UUID projectId, @Param("status") TaskStatus status);

    /**
     * Vị trí lớn nhất trong danh sách checklist con.
     *
     * @param parentTaskId task cha
     * @return vị trí lớn nhất (0 nếu chưa có)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    @Query("select coalesce(max(t.position), 0) from Task t "
            + "where t.parentTaskId = :parentTaskId and t.deletedAt is null")
    double maxSiblingPosition(@Param("parentTaskId") UUID parentTaskId);
}
