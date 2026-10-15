package com.example.remoteapp.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository cho {@link Project}.
 *
 * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
 */
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    /**
     * Tìm dự án chưa bị xoá theo id.
     *
     * @param id id dự án
     * @return dự án nếu tồn tại và chưa soft delete
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B1
     */
    Optional<Project> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * Danh sách dự án mà một người dùng là thành viên (mới tạo trước).
     *
     * @param userId người dùng
     * @return danh sách dự án
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @Query("select p from Project p, ProjectMember m "
            + "where p.id = m.projectId and m.userId = :userId and p.deletedAt is null "
            + "order by p.createdAt desc")
    List<Project> findAllByMemberUserId(@Param("userId") UUID userId);
}
