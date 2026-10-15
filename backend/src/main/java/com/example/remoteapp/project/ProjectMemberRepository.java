package com.example.remoteapp.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho {@link ProjectMember}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2
 */
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    /**
     * Tìm quan hệ thành viên theo dự án + người dùng.
     *
     * @param projectId dự án
     * @param userId    người dùng
     * @return thành viên nếu có
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2
     */
    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    /**
     * Danh sách thành viên của dự án (sắp theo thời điểm tham gia).
     *
     * @param projectId dự án
     * @return danh sách thành viên
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    List<ProjectMember> findByProjectIdOrderByJoinedAtAsc(UUID projectId);

    /**
     * Kiểm tra một người đã là thành viên dự án chưa.
     *
     * @param projectId dự án
     * @param userId    người dùng
     * @return true nếu đã là thành viên
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3
     */
    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    /**
     * Đếm số thành viên giữ một vai trò (chặn xoá/đổi OWNER cuối cùng).
     *
     * @param projectId dự án
     * @param role      vai trò cần đếm
     * @return số thành viên
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2
     */
    long countByProjectIdAndRole(UUID projectId, ProjectRole role);
}
