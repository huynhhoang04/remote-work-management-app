package com.example.remoteapp.task;

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
import com.example.remoteapp.file.FileService;
import com.example.remoteapp.notification.NotificationService;
import com.example.remoteapp.project.Project;
import com.example.remoteapp.project.ProjectMember;
import com.example.remoteapp.project.ProjectMemberRepository;
import com.example.remoteapp.project.ProjectPermissionService;
import com.example.remoteapp.project.ProjectRepository;
import com.example.remoteapp.project.ProjectRole;
import com.example.remoteapp.project.ProjectService;
import com.example.remoteapp.project.dto.ProjectMemberResponse;
import com.example.remoteapp.task.dto.AssigneesRequest;
import com.example.remoteapp.task.dto.TaskRequest;
import com.example.remoteapp.task.dto.TaskUpdateRequest;
import com.example.remoteapp.user.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test cho {@link TaskService} — validate deadline, parent, assignee, version,
 * trạng thái và soft delete.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1, §C2
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskAssigneeRepository taskAssigneeRepository;
    @Mock private TaskLabelRepository taskLabelRepository;
    @Mock private LabelRepository labelRepository;
    @Mock private TaskCommentRepository taskCommentRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private ProjectService projectService;
    @Mock private UserService userService;
    @Mock private NotificationService notificationService;
    @Mock private FileService fileService;
    @Mock private AuditService auditService;

    private TaskService taskService;
    private Project project;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ProjectPermissionService permissionService =
                new ProjectPermissionService(projectRepository, projectMemberRepository);
        taskService = new TaskService(taskRepository, taskAssigneeRepository, taskLabelRepository,
                labelRepository, taskCommentRepository, permissionService, projectService,
                userService, notificationService, fileService, auditService);
        ownerId = UUID.randomUUID();
        project = new Project();
        project.setId(UUID.randomUUID());
        project.setName("Do an");
        project.setOwnerId(ownerId);
    }

    private void givenMember(UUID userId, ProjectRole role) {
        ProjectMember member = new ProjectMember();
        member.setId(UUID.randomUUID());
        member.setProjectId(project.getId());
        member.setUserId(userId);
        member.setRole(role);
        when(projectRepository.findByIdAndDeletedAtIsNull(project.getId()))
                .thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(project.getId(), userId))
                .thenReturn(Optional.of(member));
    }

    private Task task(UUID taskId, TaskStatus status) {
        Task task = new Task();
        task.setId(taskId);
        task.setProjectId(project.getId());
        task.setTitle("Task");
        task.setStatus(status);
        task.setPriority(TaskPriority.MEDIUM);
        task.setCreatedBy(ownerId);
        task.setPosition(1000);
        return task;
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1 */
    @Test
    void create_rejectsStartDateAfterDeadline() {
        givenMember(ownerId, ProjectRole.OWNER);
        TaskRequest request = new TaskRequest(project.getId(), "Task", null, null, null,
                Instant.parse("2026-11-02T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"),
                null, null, null, null, null);

        assertThatThrownBy(() -> taskService.create(ownerId, request))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TASK_DEADLINE_INVALID));
        verify(taskRepository, never()).save(any(Task.class));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1 */
    @Test
    void create_rejectsNestedSubtaskParent() {
        givenMember(ownerId, ProjectRole.OWNER);
        UUID parentId = UUID.randomUUID();
        Task parent = task(parentId, TaskStatus.TODO);
        parent.setParentTaskId(UUID.randomUUID());
        when(taskRepository.findByIdAndDeletedAtIsNull(parentId)).thenReturn(Optional.of(parent));
        TaskRequest request = new TaskRequest(project.getId(), "Con", null, null, null, null, null,
                null, null, parentId, null, null);

        assertThatThrownBy(() -> taskService.create(ownerId, request))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TASK_PARENT_INVALID));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C2 */
    @Test
    void create_rejectsAssigneeOutsideProject() {
        givenMember(ownerId, ProjectRole.OWNER);
        when(projectService.listMembers(project.getId(), ownerId)).thenReturn(List.of(
                new ProjectMemberResponse(ownerId, "Owner", "owner@example.com", "OWNER",
                        Instant.now())));
        TaskRequest request = new TaskRequest(project.getId(), "Task", null, null, null, null, null,
                null, List.of(UUID.randomUUID()), null, null, null);

        assertThatThrownBy(() -> taskService.create(ownerId, request))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TASK_ASSIGNEE_INVALID));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1 */
    @Test
    void update_rejectsStaleVersion() {
        UUID taskId = UUID.randomUUID();
        Task task = task(taskId, TaskStatus.TODO);
        task.setVersion(3);
        givenMember(ownerId, ProjectRole.OWNER);
        when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.update(taskId, ownerId,
                new TaskUpdateRequest("Moi", null, null, null, null, null, null, null, null, 2L)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TASK_VERSION_CONFLICT));
        verify(taskRepository, never()).save(task);
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1 */
    @Test
    void changeStatus_setsAndClearsCompletedAt() {
        UUID taskId = UUID.randomUUID();
        Task task = task(taskId, TaskStatus.TODO);
        givenMember(ownerId, ProjectRole.OWNER);
        when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.of(task));

        taskService.changeStatus(taskId, ownerId, TaskStatus.DONE);
        assertThat(task.getCompletedAt()).isNotNull();
        verify(auditService).record(eq(ownerId), eq(project.getId()),
                eq(AuditAction.TASK_STATUS_CHANGED), eq("TASK"), eq(taskId), eq("status=DONE"));

        taskService.changeStatus(taskId, ownerId, TaskStatus.TODO);
        assertThat(task.getCompletedAt()).isNull();
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1 */
    @Test
    void delete_softDeletesTaskAndSubtasks() {
        UUID taskId = UUID.randomUUID();
        Task task = task(taskId, TaskStatus.TODO);
        Task subtask = task(UUID.randomUUID(), TaskStatus.TODO);
        subtask.setParentTaskId(taskId);
        givenMember(ownerId, ProjectRole.OWNER);
        when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.findByParentTaskId(taskId)).thenReturn(List.of(subtask));

        taskService.delete(taskId, ownerId);

        assertThat(task.getDeletedAt()).isNotNull();
        assertThat(subtask.getDeletedAt()).isNotNull();
        verify(auditService).record(eq(ownerId), eq(project.getId()), eq(AuditAction.TASK_DELETED),
                eq("TASK"), eq(taskId), isNull());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C2 */
    @Test
    void setAssignees_notifiesOnlyNewAssignees() {
        UUID taskId = UUID.randomUUID();
        UUID newUserId = UUID.randomUUID();
        Task task = task(taskId, TaskStatus.TODO);
        givenMember(ownerId, ProjectRole.OWNER);
        when(taskRepository.findByIdAndDeletedAtIsNull(taskId)).thenReturn(Optional.of(task));
        when(taskAssigneeRepository.findByTaskId(taskId)).thenReturn(List.of());
        when(projectService.listMembers(project.getId(), ownerId)).thenReturn(List.of(
                new ProjectMemberResponse(ownerId, "Owner", "o@example.com", "OWNER", Instant.now()),
                new ProjectMemberResponse(newUserId, "Member", "m@example.com", "MEMBER",
                        Instant.now())));

        taskService.setAssignees(taskId, ownerId, new AssigneesRequest(List.of(newUserId)));

        verify(notificationService).notify(eq(newUserId), any(), any(), any(), eq("TASK"),
                eq(taskId), isNull());
        verify(notificationService, never()).notify(eq(ownerId), any(), any(), any(), any(), any(),
                any());
    }
}
