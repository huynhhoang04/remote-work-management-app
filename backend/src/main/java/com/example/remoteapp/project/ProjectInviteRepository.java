package com.example.remoteapp.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho {@link ProjectInvite}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3
 */
public interface ProjectInviteRepository extends JpaRepository<ProjectInvite, UUID> {

    /**
     * Tìm link mời theo mã.
     *
     * @param code mã mời từ client
     * @return link mời nếu tồn tại
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3
     */
    Optional<ProjectInvite> findByCode(String code);

    /**
     * Danh sách link mời còn hiệu lực của dự án.
     *
     * @param projectId dự án
     * @return danh sách link chưa thu hồi
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    List<ProjectInvite> findByProjectIdAndRevokedFalseOrderByCreatedAtDesc(UUID projectId);

    /**
     * Tìm link mời theo id + dự án (kiểm tra quyền sở hữu khi thu hồi).
     *
     * @param inviteId  id link mời
     * @param projectId dự án
     * @return link mời nếu có
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3
     */
    Optional<ProjectInvite> findByIdAndProjectId(UUID inviteId, UUID projectId);
}
