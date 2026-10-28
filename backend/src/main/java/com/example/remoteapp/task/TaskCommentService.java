package com.example.remoteapp.task;

import com.example.remoteapp.audit.AuditAction;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ bình luận task + @mention (plan 03 §C3).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3
 */
@Service
public class TaskCommentService {

    private static final String ENTITY_TYPE = "COMMENT";
    private static final int MENTION_PREVIEW_LENGTH = 100;

    private final TaskService taskService;
    private final TaskCommentRepository commentRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    /**
     * Khởi tạo service (constructor injection tường minh).
     *
     * @param taskService         nghiệp vụ task (kiểm quyền)
     * @param commentRepository   repository bình luận
     * @param projectService      danh sách thành viên (validate mention)
     * @param userService         ghép tên tác giả
     * @param notificationService thông báo khi được mention
     * @param auditService        ghi audit log
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public TaskCommentService(TaskService taskService, TaskCommentRepository commentRepository,
                              ProjectService projectService, UserService userService,
                              NotificationService notificationService, AuditService auditService) {
        this.taskService = taskService;
        this.commentRepository = commentRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    /**
     * Danh sách bình luận của task (cũ nhất trước).
     *
     * @param taskId  id task
     * @param actorId người yêu cầu
     * @return danh sách bình luận
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> list(UUID taskId, UUID actorId) {
        taskService.requireAccessible(taskId, actorId);
        List<TaskComment> comments = commentRepository
                .findByTaskIdAndDeletedAtIsNullOrderByCreatedAtAsc(taskId);
        Map<UUID, String> names = userService.namesByIds(comments.stream()
                .map(TaskComment::getUserId).collect(Collectors.toSet()));
        return comments.stream()
                .map(comment -> CommentResponse.from(comment,
                        names.getOrDefault(comment.getUserId(), "")))
                .toList();
    }

    /**
     * Thêm bình luận; mention tạo thông báo cho thành viên dự án (trừ chính mình).
     *
     * @param taskId  id task
     * @param actorId tác giả
     * @param request nội dung + mentionUserIds
     * @return bình luận mới
     * @throws AppException TASK_ASSIGNEE_INVALID nếu mention người ngoài dự án
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3
     */
    @Transactional
    public CommentResponse add(UUID taskId, UUID actorId, CommentRequest request) {
        Task task = taskService.requireWritable(taskId, actorId);
        TaskComment comment = new TaskComment();
        comment.setTaskId(taskId);
        comment.setUserId(actorId);
        comment.setContent(request.content().trim());
        commentRepository.save(comment);

        auditService.record(actorId, task.getProjectId(), AuditAction.COMMENT_CREATED, ENTITY_TYPE,
                comment.getId(), "task=" + taskId);

        Set<UUID> mentions = resolveMentions(task.getProjectId(), actorId, request.mentionUserIds());
        for (UUID userId : mentions) {
            notificationService.notify(userId, NotificationType.MENTION,
                    "Bạn được nhắc đến trong một bình luận", preview(comment.getContent()),
                    "TASK", taskId, null);
        }

        Map<UUID, String> names = userService.namesByIds(Set.of(actorId));
        return CommentResponse.from(comment, names.getOrDefault(actorId, ""));
    }

    /**
     * Sửa bình luận — chỉ tác giả.
     *
     * @param taskId    id task
     * @param commentId id bình luận
     * @param actorId   người yêu cầu
     * @param request   nội dung mới
     * @throws AppException COMMENT_NOT_FOUND / COMMENT_FORBIDDEN
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3
     */
    @Transactional
    public void update(UUID taskId, UUID commentId, UUID actorId, CommentRequest request) {
        taskService.requireWritable(taskId, actorId);
        TaskComment comment = findComment(taskId, commentId);
        if (!comment.getUserId().equals(actorId)) {
            throw new AppException(ErrorCode.COMMENT_FORBIDDEN,
                    "Chỉ tác giả mới được sửa bình luận");
        }
        comment.setContent(request.content().trim());
        commentRepository.save(comment);
    }

    /**
     * Xoá bình luận (soft delete) — chỉ tác giả.
     *
     * @param taskId    id task
     * @param commentId id bình luận
     * @param actorId   người yêu cầu
     * @throws AppException COMMENT_NOT_FOUND / COMMENT_FORBIDDEN
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3
     */
    @Transactional
    public void delete(UUID taskId, UUID commentId, UUID actorId) {
        taskService.requireWritable(taskId, actorId);
        TaskComment comment = findComment(taskId, commentId);
        if (!comment.getUserId().equals(actorId)) {
            throw new AppException(ErrorCode.COMMENT_FORBIDDEN,
                    "Chỉ tác giả mới được xoá bình luận");
        }
        comment.setDeletedAt(Instant.now());
        commentRepository.save(comment);
    }

    /**
     * Tìm bình luận còn hiển thị và thuộc đúng task.
     *
     * @param taskId    id task
     * @param commentId id bình luận
     * @return entity bình luận
     * @throws AppException COMMENT_NOT_FOUND
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3
     */
    private TaskComment findComment(UUID taskId, UUID commentId) {
        TaskComment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND,
                        "Không tìm thấy bình luận"));
        if (!taskId.equals(comment.getTaskId())) {
            throw new AppException(ErrorCode.COMMENT_NOT_FOUND, "Không tìm thấy bình luận");
        }
        return comment;
    }

    /**
     * Validate người được mention là thành viên dự án, bỏ chính tác giả.
     *
     * @param projectId dự án
     * @param actorId   tác giả
     * @param ids       danh sách id client gửi
     * @return danh sách id hợp lệ (duy nhất)
     * @throws AppException TASK_ASSIGNEE_INVALID nếu có người ngoài dự án
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3
     */
    private Set<UUID> resolveMentions(UUID projectId, UUID actorId, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<UUID> requested = new LinkedHashSet<>(ids);
        requested.remove(actorId);
        if (requested.isEmpty()) {
            return Set.of();
        }
        Set<UUID> memberIds = projectService.listMembers(projectId, actorId).stream()
                .map(ProjectMemberResponse::userId)
                .collect(Collectors.toSet());
        if (!memberIds.containsAll(requested)) {
            throw new AppException(ErrorCode.TASK_ASSIGNEE_INVALID,
                    "Người được nhắc phải là thành viên dự án");
        }
        return requested;
    }

    /**
     * Cắt nội dung mention làm preview thông báo.
     *
     * @param content nội dung bình luận
     * @return preview ≤ 100 ký tự
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3
     */
    private String preview(String content) {
        return content.length() <= MENTION_PREVIEW_LENGTH
                ? content
                : content.substring(0, MENTION_PREVIEW_LENGTH) + "...";
    }
}
