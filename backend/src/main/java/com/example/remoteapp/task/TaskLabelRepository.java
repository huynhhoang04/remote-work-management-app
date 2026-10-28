package com.example.remoteapp.task;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho {@link TaskLabel}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
 */
public interface TaskLabelRepository extends JpaRepository<TaskLabel, UUID> {

    /**
     * Nhãn của một task.
     *
     * @param taskId id task
     * @return danh sách liên kết
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    List<TaskLabel> findByTaskId(UUID taskId);

    /**
     * Nhãn của nhiều task (dựng danh sách Kanban không N+1).
     *
     * @param taskIds danh sách task
     * @return danh sách liên kết
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    List<TaskLabel> findByTaskIdIn(Collection<UUID> taskIds);

    /**
     * Xoá toàn bộ nhãn của task.
     *
     * @param taskId id task
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    void deleteByTaskId(UUID taskId);

    /**
     * Xoá một nhãn khỏi task.
     *
     * @param taskId  id task
     * @param labelId id nhãn
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    void deleteByTaskIdAndLabelId(UUID taskId, UUID labelId);

    /**
     * Đếm số task đang dùng nhãn (chặn xoá nhãn còn dùng).
     *
     * @param labelId id nhãn
     * @return số liên kết
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    long countByLabelId(UUID labelId);

    /**
     * Xoá mọi liên kết của một nhãn (khi xoá nhãn khỏi dự án).
     *
     * @param labelId id nhãn
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    void deleteByLabelId(UUID labelId);
}
