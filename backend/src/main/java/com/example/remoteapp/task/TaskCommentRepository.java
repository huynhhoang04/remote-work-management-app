package com.example.remoteapp.task;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository cho {@link TaskComment}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3
 */
public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {

    /**
     * Bình luận còn hiển thị của task (cũ nhất trước).
     *
     * @param taskId id task
     * @return danh sách bình luận
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    List<TaskComment> findByTaskIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID taskId);

    /**
     * Tìm bình luận chưa bị xoá.
     *
     * @param id id bình luận
     * @return bình luận nếu tồn tại
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3
     */
    Optional<TaskComment> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * Đếm bình luận của một task.
     *
     * @param taskId id task
     * @return số bình luận
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    long countByTaskIdAndDeletedAtIsNull(UUID taskId);

    /**
     * Đếm bình luận cho nhiều task trong một query.
     *
     * @param taskIds danh sách task
     * @return cặp (taskId, count)
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @Query("select c.taskId as taskId, count(c) as count from TaskComment c "
            + "where c.taskId in :taskIds and c.deletedAt is null group by c.taskId")
    List<TaskCommentCount> countByTaskIds(@Param("taskIds") Collection<UUID> taskIds);

    /**
     * Projection đếm bình luận theo task.
     *
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    interface TaskCommentCount {

        /**
         * Id task.
         *
         * @return id task
         * @reference docs/plan-backend/02-danh-sach-api.md §5
         */
        UUID getTaskId();

        /**
         * Số bình luận.
         *
         * @return số bình luận
         * @reference docs/plan-backend/02-danh-sach-api.md §5
         */
        Long getCount();
    }
}
