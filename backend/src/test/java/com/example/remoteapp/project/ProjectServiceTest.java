package com.example.remoteapp.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.remoteapp.audit.AuditAction;
import com.example.remoteapp.audit.AuditService;
import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.project.dto.ProjectRequest;
import com.example.remoteapp.project.dto.ProjectResponse;
import com.example.remoteapp.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test cho {@link ProjectService} + {@link ProjectPermissionService} (RBAC).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B1, §B2
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;

    private ProjectPermissionService permissionService;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        permissionService = new ProjectPermissionService(projectRepository, projectMemberRepository);
        projectService = new ProjectService(projectRepository, projectMemberRepository, permissionService,
                userRepository, auditService);
    }

    private Project project() {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setName("Do an Mobile");
        project.setOwnerId(UUID.randomUUID());
        return project;
    }

    private ProjectMember member(UUID projectId, UUID userId, ProjectRole role) {
        ProjectMember member = new ProjectMember();
        member.setId(UUID.randomUUID());
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B1 */
    @Test
    void create_makesCreatorOwnerAndAudits() {
        UUID actorId = UUID.randomUUID();
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.create(actorId, new ProjectRequest("Do an Mobile", "mo ta"));

        assertThat(response.role()).isEqualTo("OWNER");
        assertThat(response.ownerId()).isEqualTo(actorId);
        verify(auditService).record(eq(actorId), eq(response.id()), eq(AuditAction.PROJECT_CREATED),
                eq("PROJECT"), eq(response.id()), any());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2 */
    @Test
    void get_forbidsNonMember() {
        Project project = project();
        UUID actorId = UUID.randomUUID();
        when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(project.getId(), actorId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.get(project.getId(), actorId))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROJECT_FORBIDDEN));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2 */
    @Test
    void update_requiresAdminRole() {
        Project project = project();
        UUID actorId = UUID.randomUUID();
        when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(project.getId(), actorId))
                .thenReturn(Optional.of(member(project.getId(), actorId, ProjectRole.MEMBER)));

        assertThatThrownBy(() -> projectService.update(project.getId(), actorId, new ProjectRequest("Ten moi", null)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROJECT_FORBIDDEN));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B1 */
    @Test
    void delete_onlyOwnerAndSoftDeletes() {
        Project project = project();
        UUID actorId = UUID.randomUUID();
        when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));

        when(projectMemberRepository.findByProjectIdAndUserId(project.getId(), actorId))
                .thenReturn(Optional.of(member(project.getId(), actorId, ProjectRole.ADMIN)));
        assertThatThrownBy(() -> projectService.delete(project.getId(), actorId))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROJECT_FORBIDDEN));

        when(projectMemberRepository.findByProjectIdAndUserId(project.getId(), actorId))
                .thenReturn(Optional.of(member(project.getId(), actorId, ProjectRole.OWNER)));
        projectService.delete(project.getId(), actorId);

        assertThat(project.getDeletedAt()).isBeforeOrEqualTo(Instant.now());
        verify(projectRepository).save(project);
        verify(auditService).record(eq(actorId), eq(project.getId()), eq(AuditAction.PROJECT_DELETED),
                eq("PROJECT"), eq(project.getId()), isNull());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2 */
    @Test
    void changeRole_protectsOwnerAndOwnerRole() {
        Project project = project();
        UUID actorId = UUID.randomUUID();
        UUID ownerId = project.getOwnerId();
        when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(project.getId(), actorId))
                .thenReturn(Optional.of(member(project.getId(), actorId, ProjectRole.ADMIN)));
        when(projectMemberRepository.findByProjectIdAndUserId(project.getId(), ownerId))
                .thenReturn(Optional.of(member(project.getId(), ownerId, ProjectRole.OWNER)));

        assertThatThrownBy(() -> projectService.changeRole(project.getId(), actorId, ownerId, ProjectRole.MEMBER))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEMBER_ROLE_INVALID));

        UUID memberId = UUID.randomUUID();
        assertThatThrownBy(() -> projectService.changeRole(project.getId(), actorId, memberId, ProjectRole.OWNER))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEMBER_ROLE_INVALID));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2 */
    @Test
    void changeRole_updatesRoleAndAudits() {
        Project project = project();
        UUID actorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        ProjectMember target = member(project.getId(), memberId, ProjectRole.MEMBER);
        when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(project.getId(), actorId))
                .thenReturn(Optional.of(member(project.getId(), actorId, ProjectRole.ADMIN)));
        when(projectMemberRepository.findByProjectIdAndUserId(project.getId(), memberId))
                .thenReturn(Optional.of(target));

        projectService.changeRole(project.getId(), actorId, memberId, ProjectRole.VIEWER);

        assertThat(target.getRole()).isEqualTo(ProjectRole.VIEWER);
        verify(projectMemberRepository).save(target);
        verify(auditService).record(eq(actorId), eq(project.getId()), eq(AuditAction.ROLE_CHANGED),
                eq("MEMBER"), eq(memberId), any());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2 */
    @Test
    void removeMember_cannotRemoveOwner_butSelfLeaveAllowed() {
        Project project = project();
        UUID actorId = UUID.randomUUID();
        UUID ownerId = project.getOwnerId();
        when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(project.getId(), actorId))
                .thenReturn(Optional.of(member(project.getId(), actorId, ProjectRole.ADMIN)));
        when(projectMemberRepository.findByProjectIdAndUserId(project.getId(), ownerId))
                .thenReturn(Optional.of(member(project.getId(), ownerId, ProjectRole.OWNER)));

        assertThatThrownBy(() -> projectService.removeMember(project.getId(), actorId, ownerId))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEMBER_ROLE_INVALID));

        ProjectMember self = member(project.getId(), actorId, ProjectRole.VIEWER);
        when(projectMemberRepository.findByProjectIdAndUserId(project.getId(), actorId))
                .thenReturn(Optional.of(self));
        projectService.removeMember(project.getId(), actorId, actorId);

        verify(projectMemberRepository).delete(self);
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §G */
    @Test
    void listAuditLogs_ownerOnly() {
        Project project = project();
        UUID actorId = UUID.randomUUID();
        when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(project.getId(), actorId))
                .thenReturn(Optional.of(member(project.getId(), actorId, ProjectRole.ADMIN)));

        assertThatThrownBy(() -> projectService.listAuditLogs(project.getId(), actorId, null,
                org.springframework.data.domain.PageRequest.of(0, 10)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROJECT_FORBIDDEN));
        verify(auditService, never()).listByProject(any(), any(), any());
    }
}
