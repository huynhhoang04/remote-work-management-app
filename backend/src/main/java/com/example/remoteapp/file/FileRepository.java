package com.example.remoteapp.file;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho {@link FileMeta}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
 */
public interface FileRepository extends JpaRepository<FileMeta, UUID> {

    /**
     * Liệt kê file đính kèm của một task.
     *
     * @param taskId id task
     * @return danh sách file (cũ nhất trước)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    List<FileMeta> findByTaskIdOrderByCreatedAtAsc(UUID taskId);

    /**
     * Liệt kê file đính kèm của một tin nhắn.
     *
     * @param messageId id tin nhắn
     * @return danh sách file
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    List<FileMeta> findByMessageIdOrderByCreatedAtAsc(UUID messageId);
}
