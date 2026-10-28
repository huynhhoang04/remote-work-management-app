package com.example.remoteapp.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho {@link Label}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
 */
public interface LabelRepository extends JpaRepository<Label, UUID> {

    /**
     * Nhãn của dự án (cũ nhất trước).
     *
     * @param projectId dự án
     * @return danh sách nhãn
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    List<Label> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    /**
     * Tìm nhãn thuộc đúng dự án.
     *
     * @param id        id nhãn
     * @param projectId dự án
     * @return nhãn nếu khớp
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    Optional<Label> findByIdAndProjectId(UUID id, UUID projectId);

    /**
     * Kiểm tra trùng tên nhãn trong dự án.
     *
     * @param projectId dự án
     * @param name      tên nhãn
     * @return true nếu đã tồn tại
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);
}
