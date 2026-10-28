package com.example.remoteapp.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.remoteapp.audit.AuditService;
import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.notification.NotificationService;
import com.example.remoteapp.notification.NotificationType;
import com.example.remoteapp.project.ProjectService;
import com.example.remoteapp.project.dto.ProjectMemberResponse;
import com.example.remoteapp.task.dto.CommentRequest;
import com.example.remoteapp.task.dto.CommentResponse;
import com.example.remoteapp.user.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test cho {@link TaskCommentService} — mention và quyền tác giả.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3
 */
@ExtendWith(MockitoExtension.class)
class TaskCommentServiceTest {

    @Mock private TaskService taskService;
    @Mock private TaskCommentRepository commentRepository;
    @Mock private ProjectService projectService;
    @Mock private UserService userService;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    private TaskCommentService commentService;
    private Task task;
    private UUID actorId;
    private UUID otherId;

    @BeforeEach
    void setUp() {
        commentService = new TaskCommentService(taskService, commentRepository, projectService,
                userService, notificationService, auditService);
        actorId = UUID.randomUUID();
        otherId = UUID.randomUUID();
        task = new Task();
        task.setId(UUID.randomUUID());
        task.setProjectId(UUID.randomUUID());
        task.setTitle("Task");
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3 */
    @Test
    void add_mentionsOtherMembersOnly() {
        when(taskService.requireWritable(task.getId(), actorId)).thenReturn(task);
        when(projectService.listMembers(task.getProjectId(), actorId)).thenReturn(List.of(
                new ProjectMemberResponse(actorId, "Actor", "a@example.com", "OWNER", Instant.now()),
                new ProjectMemberResponse(otherId, "Other", "o@example.com", "MEMBER",
                        Instant.now())));
        when(userService.namesByIds(Set.of(actorId))).thenReturn(Map.of(actorId, "Actor"));
        when(commentRepository.save(any(TaskComment.class))).thenAnswer(invocation -> {
            TaskComment saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        CommentResponse response = commentService.add(task.getId(), actorId,
                new CommentRequest("Xin chào @Other", List.of(actorId, otherId)));

        assertThat(response.content()).isEqualTo("Xin chào @Other");
        verify(notificationService).notify(eq(otherId), eq(NotificationType.MENTION), any(),
                any(), eq("TASK"), eq(task.getId()), isNull());
        verify(notificationService, never()).notify(eq(actorId), any(), any(), any(), any(), any(),
                any());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3 */
    @Test
    void update_rejectsNonAuthor() {
        when(taskService.requireWritable(task.getId(), actorId)).thenReturn(task);
        TaskComment comment = new TaskComment();
        comment.setId(UUID.randomUUID());
        comment.setTaskId(task.getId());
        comment.setUserId(otherId);
        comment.setContent("cua nguoi khac");
        when(commentRepository.findByIdAndDeletedAtIsNull(comment.getId()))
                .thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(task.getId(), comment.getId(), actorId,
                new CommentRequest("sua", null)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COMMENT_FORBIDDEN));
        verify(commentRepository, never()).save(comment);
    }
}
