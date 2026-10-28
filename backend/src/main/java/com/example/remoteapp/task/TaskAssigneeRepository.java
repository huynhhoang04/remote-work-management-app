package com.example.remoteapp.task;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho {@link TaskAssignee}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C2
 */
public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, UUID> {

    /**
     * Danh sách người được gán của task.
     *
     * @param taskId id task
     * @return danh sách assignee
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C2
     */
    List<TaskAssignee> findByTaskId(UUID taskId);

    /**
     * Assignee của nhiều task (dựng danh sách Kanban không N+1).
     *
     * @param taskIds danh sách task
     * @return danh sách assignee
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    List<TaskAssignee> findByTaskIdIn(Collection<UUID> taskIds);

    /**
     * Tìm quan hệ gán cụ thể.
     *
     * @param taskId id task
     * @param userId id người dùng
     * @return quan hệ nếu có
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C2
     */
    Optional<TaskAssignee> findByTaskIdAndUserId(UUID taskId, UUID userId);

    /**
     * Xoá một người khỏi task.
     *
     * @param taskId id task
     * @param userId id người dùng
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C2
     */
    void deleteByTaskIdAndUserId(UUID taskId, UUID userId);

    /**
     * Xoá toàn bộ assignee của task.
     *
     * @param taskId id task
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C2
     */
    void deleteByTaskId(UUID taskId);
}
