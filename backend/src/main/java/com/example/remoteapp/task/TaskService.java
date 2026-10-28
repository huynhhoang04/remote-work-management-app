package com.example.remoteapp.task;

import com.example.remoteapp.audit.AuditAction;
import com.example.remoteapp.audit.AuditService;
import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.file.FileService;
import com.example.remoteapp.file.dto.FileResponse;
import com.example.remoteapp.notification.NotificationService;
import com.example.remoteapp.notification.NotificationType;
import com.example.remoteapp.project.ProjectPermissionService;
import com.example.remoteapp.project.ProjectRole;
import com.example.remoteapp.project.ProjectService;
import com.example.remoteapp.project.dto.ProjectMemberResponse;
import com.example.remoteapp.task.dto.AssigneesRequest;
import com.example.remoteapp.task.dto.LabelRequest;
import com.example.remoteapp.task.dto.LabelResponse;
import com.example.remoteapp.task.dto.PositionRequest;
import com.example.remoteapp.task.dto.SubtaskRequest;
import com.example.remoteapp.task.dto.SubtaskUpdateRequest;
import com.example.remoteapp.task.dto.TaskRequest;
import com.example.remoteapp.task.dto.TaskResponse;
import com.example.remoteapp.task.dto.TaskUpdateRequest;
import com.example.remoteapp.user.UserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ task: CRUD, gán người, trạng thái Kanban, checklist, nhãn,
 * file đính kèm (plan 03 §C1–§C3).
 *
 * <p>Mọi thao tác ghi yêu cầu vai trò tối thiểu MEMBER; VIEWER chỉ đọc.
 * Deadline quá khứ vẫn cho lưu (client tự cảnh báo) — chỉ chặn
 * {@code startDate > deadline}.</p>
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1, §C2, §C3
 */
@Service
public class TaskService {

    private static final String ENTITY_TYPE = "TASK";

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final TaskLabelRepository taskLabelRepository;
    private final LabelRepository labelRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final ProjectPermissionService permissionService;
    private final ProjectService projectService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final FileService fileService;
    private final AuditService auditService;

    /**
     * Khởi tạo service (constructor injection tường minh).
     *
     * @param taskRepository          repository task
     * @param taskAssigneeRepository  repository assignee
     * @param taskLabelRepository     repository liên kết nhãn
     * @param labelRepository         repository nhãn
     * @param taskCommentRepository   repository bình luận
     * @param permissionService       kiểm tra quyền theo dự án
     * @param projectService          đọc danh sách thành viên dự án
     * @param userService             ghép tên người dùng
     * @param notificationService     thông báo khi gán task
     * @param fileService             file đính kèm
     * @param auditService            ghi audit log
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public TaskService(TaskRepository taskRepository, TaskAssigneeRepository taskAssigneeRepository,
                       TaskLabelRepository taskLabelRepository, LabelRepository labelRepository,
                       TaskCommentRepository taskCommentRepository,
                       ProjectPermissionService permissionService, ProjectService projectService,
                       UserService userService, NotificationService notificationService,
                       FileService fileService, AuditService auditService) {
        this.taskRepository = taskRepository;
        this.taskAssigneeRepository = taskAssigneeRepository;
        this.taskLabelRepository = taskLabelRepository;
        this.labelRepository = labelRepository;
        this.taskCommentRepository = taskCommentRepository;
        this.permissionService = permissionService;
        this.projectService = projectService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.fileService = fileService;
        this.auditService = auditService;
    }

    /**
     * Toàn bộ task gốc của dự án, kèm subtask + assignee + nhãn (client tự sort/filter).
     *
     * @param projectId dự án
     * @param actorId   người yêu cầu
     * @return danh sách task gốc
     * @throws AppException PROJECT_FORBIDDEN nếu không phải thành viên
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> list(UUID projectId, UUID actorId) {
        permissionService.requireMember(projectId, actorId);
        List<Task> all = taskRepository
                .findByProjectIdAndDeletedAtIsNullOrderByPositionAscCreatedAtAsc(projectId);
        Map<UUID, List<Task>> children = all.stream()
                .filter(task -> task.getParentTaskId() != null)
                .collect(Collectors.groupingBy(Task::getParentTaskId));
        ViewData data = loadViewData(all);
        return all.stream()
                .filter(task -> task.getParentTaskId() == null)
                .map(task -> toResponse(task, children.getOrDefault(task.getId(), List.of()), data,
                        List.of()))
                .toList();
    }

    /**
     * Chi tiết task (kèm subtask, assignee, nhãn, file đính kèm).
     *
     * @param taskId  id task
     * @param actorId người yêu cầu
     * @return task DTO
     * @throws AppException TASK_NOT_FOUND / PROJECT_FORBIDDEN
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @Transactional(readOnly = true)
    public TaskResponse get(UUID taskId, UUID actorId) {
        Task task = findTask(taskId);
        permissionService.requireMember(task.getProjectId(), actorId);
        List<Task> subtasks = taskRepository
                .findByParentTaskIdAndDeletedAtIsNullOrderByPositionAscCreatedAtAsc(taskId);
        List<Task> all = new ArrayList<>(subtasks);
        all.add(task);
        ViewData data = loadViewData(all);
        return toResponse(task, subtasks, data, fileService.listByTask(taskId));
    }

    /**
     * Kiểm tra người dùng được xem task (dùng cho module schedule).
     *
     * @param taskId  id task
     * @param actorId người yêu cầu
     * @throws AppException TASK_NOT_FOUND / PROJECT_FORBIDDEN
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §D1
     */
    @Transactional(readOnly = true)
    public void requireAccessible(UUID taskId, UUID actorId) {
        Task task = findTask(taskId);
        permissionService.requireMember(task.getProjectId(), actorId);
    }

    /**
     * Lấy task và yêu cầu quyền ghi (MEMBER trở lên).
     *
     * @param taskId  id task
     * @param actorId người yêu cầu
     * @return entity task
     * @throws AppException TASK_NOT_FOUND / PROJECT_FORBIDDEN
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    @Transactional
    public Task requireWritable(UUID taskId, UUID actorId) {
        Task task = findTask(taskId);
        permissionService.requireRole(task.getProjectId(), actorId, ProjectRole.MEMBER);
        return task;
    }

    /**
     * Tạo task (hoặc task con nếu có {@code parentTaskId}).
     *
     * @param actorId người tạo
     * @param request dữ liệu task
     * @return task DTO
     * @throws AppException TASK_DEADLINE_INVALID / TASK_PARENT_INVALID / TASK_ASSIGNEE_INVALID
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1, §C2
     */
    @Transactional
    public TaskResponse create(UUID actorId, TaskRequest request) {
        permissionService.requireRole(request.projectId(), actorId, ProjectRole.MEMBER);
        validateWindow(request.startDate(), request.deadline());
        Task parent = request.parentTaskId() == null
                ? null
                : validateParent(request.projectId(), request.parentTaskId());

        TaskStatus status = request.status() == null ? TaskStatus.TODO : request.status();
        Task task = new Task();
        task.setProjectId(request.projectId());
        task.setParentTaskId(parent == null ? null : parent.getId());
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setStatus(status);
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setStartDate(request.startDate());
        task.setDeadline(request.deadline());
        task.setCreatedBy(actorId);
        task.setPosition(resolvePosition(request.position(), request.projectId(), status, parent));
        if (status == TaskStatus.DONE) {
            task.setCompletedAt(Instant.now());
        }
        taskRepository.save(task);

        List<UUID> assigneeIds = resolveAssignees(request.projectId(), actorId, request.assigneeIds());
        replaceAssignees(task.getId(), assigneeIds);
        replaceLabels(task.getId(), request.projectId(), request.labelIds());

        auditService.record(actorId, request.projectId(), AuditAction.TASK_CREATED, ENTITY_TYPE,
                task.getId(), "title=" + task.getTitle());
        if (!assigneeIds.isEmpty()) {
            auditService.record(actorId, request.projectId(), AuditAction.TASK_ASSIGNED, ENTITY_TYPE,
                    task.getId(), "assignees=" + assigneeIds.size());
            notifyAssignment(task, assigneeIds, actorId);
        }
        return get(task.getId(), actorId);
    }

    /**
     * Cập nhật task theo kiểu PATCH (trường null = giữ nguyên).
     *
     * @param taskId  id task
     * @param actorId người yêu cầu
     * @param request các trường cần sửa
     * @return task DTO sau cập nhật
     * @throws AppException TASK_VERSION_CONFLICT nếu version lệch
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    @Transactional
    public TaskResponse update(UUID taskId, UUID actorId, TaskUpdateRequest request) {
        Task task = requireWritable(taskId, actorId);
        checkVersion(task, request.version());

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Tiêu đề không được để trống");
            }
            task.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.startDate() != null) {
            task.setStartDate(request.startDate());
        }
        if (request.deadline() != null) {
            task.setDeadline(request.deadline());
        }
        if (request.status() != null) {
            applyStatus(task, request.status());
        }
        if (request.position() != null) {
            task.setPosition(request.position());
        }
        validateWindow(task.getStartDate(), task.getDeadline());
        taskRepository.save(task);

        List<UUID> added = List.of();
        if (request.assigneeIds() != null) {
            List<UUID> current = currentAssigneeIds(taskId);
            List<UUID> target = resolveAssignees(task.getProjectId(), actorId, request.assigneeIds());
            added = target.stream().filter(id -> !current.contains(id)).toList();
            replaceAssignees(taskId, target);
        }
        if (request.labelIds() != null) {
            replaceLabels(taskId, task.getProjectId(), request.labelIds());
        }

        auditService.record(actorId, task.getProjectId(), AuditAction.TASK_UPDATED, ENTITY_TYPE,
                taskId, null);
        if (!added.isEmpty()) {
            auditService.record(actorId, task.getProjectId(), AuditAction.TASK_ASSIGNED, ENTITY_TYPE,
                    taskId, "assignees=" + added.size());
            notifyAssignment(task, added, actorId);
        }
        return get(taskId, actorId);
    }

    /**
     * Đổi trạng thái task (kéo Kanban) — set/clear {@code completedAt}.
     *
     * @param taskId  id task
     * @param actorId người yêu cầu
     * @param status  trạng thái mới
     * @return task DTO
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    @Transactional
    public TaskResponse changeStatus(UUID taskId, UUID actorId, TaskStatus status) {
        Task task = requireWritable(taskId, actorId);
        TaskStatus previous = task.getStatus();
        applyStatus(task, status);
        taskRepository.save(task);
        if (previous != status) {
            auditService.record(actorId, task.getProjectId(), AuditAction.TASK_STATUS_CHANGED,
                    ENTITY_TYPE, taskId, "status=" + status.name());
        }
        return get(taskId, actorId);
    }

    /**
     * Cập nhật vị trí Kanban (kéo thả) — có thể đổi cột.
     *
     * @param taskId  id task
     * @param actorId người yêu cầu
     * @param request cột đích + vị trí
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    @Transactional
    public void changePosition(UUID taskId, UUID actorId, PositionRequest request) {
        Task task = requireWritable(taskId, actorId);
        boolean statusChanged = task.getStatus() != request.status();
        applyStatus(task, request.status());
        task.setPosition(request.position());
        taskRepository.save(task);
        if (statusChanged) {
            auditService.record(actorId, task.getProjectId(), AuditAction.TASK_STATUS_CHANGED,
                    ENTITY_TYPE, taskId, "status=" + request.status().name());
        }
    }

    /**
     * Gán lại danh sách người thực hiện (danh sách đầy đủ) — chỉ người mới nhận thông báo.
     *
     * @param taskId  id task
     * @param actorId người yêu cầu
     * @param request danh sách assignee đầy đủ
     * @return task DTO
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C2
     */
    @Transactional
    public TaskResponse setAssignees(UUID taskId, UUID actorId, AssigneesRequest request) {
        Task task = requireWritable(taskId, actorId);
        List<UUID> current = currentAssigneeIds(taskId);
        List<UUID> target = resolveAssignees(task.getProjectId(), actorId, request.assigneeIds());
        List<UUID> added = target.stream().filter(id -> !current.contains(id)).toList();
        replaceAssignees(taskId, target);
        if (!added.isEmpty()) {
            auditService.record(actorId, task.getProjectId(), AuditAction.TASK_ASSIGNED, ENTITY_TYPE,
                    taskId, "assignees=" + added.size());
            notifyAssignment(task, added, actorId);
        }
        return get(taskId, actorId);
    }

    /**
     * Xoá task (soft delete) kèm toàn bộ subtask.
     *
     * @param taskId  id task
     * @param actorId người yêu cầu
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    @Transactional
    public void delete(UUID taskId, UUID actorId) {
        Task task = requireWritable(taskId, actorId);
        List<Task> children = taskRepository.findByParentTaskId(taskId);
        Instant now = Instant.now();
        for (Task child : children) {
            if (child.getDeletedAt() == null) {
                child.setDeletedAt(now);
                taskRepository.save(child);
            }
        }
        task.setDeletedAt(now);
        taskRepository.save(task);
        auditService.record(actorId, task.getProjectId(), AuditAction.TASK_DELETED, ENTITY_TYPE,
                taskId, null);
    }

    /**
     * Thêm checklist con (không cho lồng cấp 2).
     *
     * @param taskId  task cha
     * @param actorId người yêu cầu
     * @param request tiêu đề subtask
     * @return DTO subtask
     * @throws AppException TASK_PARENT_INVALID nếu task cha là subtask
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    @Transactional
    public TaskResponse addSubtask(UUID taskId, UUID actorId, SubtaskRequest request) {
        Task parent = requireWritable(taskId, actorId);
        if (parent.getParentTaskId() != null) {
            throw new AppException(ErrorCode.TASK_PARENT_INVALID,
                    "Không thể tạo subtask lồng trong subtask");
        }
        Task subtask = new Task();
        subtask.setProjectId(parent.getProjectId());
        subtask.setParentTaskId(parent.getId());
        subtask.setTitle(request.title().trim());
        subtask.setStatus(TaskStatus.TODO);
        subtask.setPriority(parent.getPriority());
        subtask.setCreatedBy(actorId);
        subtask.setPosition(taskRepository.maxSiblingPosition(parent.getId()) + 1000);
        taskRepository.save(subtask);
        auditService.record(actorId, parent.getProjectId(), AuditAction.TASK_CREATED, ENTITY_TYPE,
                subtask.getId(), "parent=" + parent.getId());
        return get(subtask.getId(), actorId);
    }

    /**
     * Cập nhật checklist con (tiêu đề / done).
     *
     * @param taskId    task cha
     * @param subtaskId subtask
     * @param actorId   người yêu cầu
     * @param request   trường cần sửa
     * @throws AppException TASK_NOT_FOUND nếu subtask không thuộc task cha
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    @Transactional
    public void updateSubtask(UUID taskId, UUID subtaskId, UUID actorId, SubtaskUpdateRequest request) {
        requireWritable(taskId, actorId);
        Task subtask = findSubtask(taskId, subtaskId);
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Tiêu đề không được để trống");
            }
            subtask.setTitle(request.title().trim());
        }
        if (request.done() != null) {
            applyStatus(subtask, request.done() ? TaskStatus.DONE : TaskStatus.TODO);
        }
        taskRepository.save(subtask);
        auditService.record(actorId, subtask.getProjectId(), AuditAction.TASK_UPDATED, ENTITY_TYPE,
                subtaskId, "subtask");
    }

    /**
     * Xoá checklist con (soft delete).
     *
     * @param taskId    task cha
     * @param subtaskId subtask
     * @param actorId   người yêu cầu
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    @Transactional
    public void deleteSubtask(UUID taskId, UUID subtaskId, UUID actorId) {
        requireWritable(taskId, actorId);
        Task subtask = findSubtask(taskId, subtaskId);
        subtask.setDeletedAt(Instant.now());
        taskRepository.save(subtask);
        auditService.record(actorId, subtask.getProjectId(), AuditAction.TASK_DELETED, ENTITY_TYPE,
                subtaskId, "subtask");
    }

    /**
     * Danh sách nhãn của dự án.
     *
     * @param projectId dự án
     * @param actorId   người yêu cầu
     * @return danh sách nhãn
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @Transactional(readOnly = true)
    public List<LabelResponse> listLabels(UUID projectId, UUID actorId) {
        permissionService.requireMember(projectId, actorId);
        return labelRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(LabelResponse::from)
                .toList();
    }

    /**
     * Tạo nhãn mới trong dự án.
     *
     * @param projectId dự án
     * @param actorId   người yêu cầu
     * @param request   tên + màu
     * @return nhãn mới
     * @throws AppException LABEL_DUPLICATE nếu trùng tên
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    @Transactional
    public LabelResponse createLabel(UUID projectId, UUID actorId, LabelRequest request) {
        permissionService.requireRole(projectId, actorId, ProjectRole.MEMBER);
        String name = request.name().trim();
        if (labelRepository.existsByProjectIdAndNameIgnoreCase(projectId, name)) {
            throw new AppException(ErrorCode.LABEL_DUPLICATE, "Nhãn này đã tồn tại trong dự án");
        }
        Label label = new Label();
        label.setProjectId(projectId);
        label.setName(name);
        label.setColor(request.color().trim());
        labelRepository.save(label);
        return LabelResponse.from(label);
    }

    /**
     * Xoá nhãn khỏi dự án (gỡ khỏi mọi task đang dùng).
     *
     * @param labelId id nhãn
     * @param actorId người yêu cầu
     * @throws AppException LABEL_NOT_FOUND
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    @Transactional
    public void deleteLabel(UUID labelId, UUID actorId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new AppException(ErrorCode.LABEL_NOT_FOUND, "Không tìm thấy nhãn"));
        permissionService.requireRole(label.getProjectId(), actorId, ProjectRole.MEMBER);
        taskLabelRepository.deleteByLabelId(labelId);
        labelRepository.delete(label);
    }

    /**
     * Thêm file đính kèm vào task (ủy quyền cho module file).
     *
     * @param taskId  id task
     * @param actorId người upload
     * @param file    file multipart
     * @return metadata file
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3
     */
    @Transactional
    public FileResponse addAttachment(UUID taskId, UUID actorId,
                                      org.springframework.web.multipart.MultipartFile file) {
        Task task = requireWritable(taskId, actorId);
        return fileService.storeAttachment(actorId, file, task.getProjectId(), taskId, null);
    }

    /**
     * Xoá file đính kèm của task.
     *
     * @param taskId  id task
     * @param fileId  id file
     * @param actorId người yêu cầu
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C3
     */
    @Transactional
    public void deleteAttachment(UUID taskId, UUID fileId, UUID actorId) {
        Task task = requireWritable(taskId, actorId);
        fileService.deleteAttachment(actorId, taskId, fileId);
    }

    /**
     * Tìm task chưa bị xoá hoặc ném lỗi.
     *
     * @param taskId id task
     * @return entity task
     * @throws AppException TASK_NOT_FOUND
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    private Task findTask(UUID taskId) {
        return taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND, "Không tìm thấy task"));
    }

    /**
     * Tìm subtask và đảm bảo thuộc đúng task cha.
     *
     * @param taskId    task cha
     * @param subtaskId subtask
     * @return entity subtask
     * @throws AppException TASK_NOT_FOUND
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    private Task findSubtask(UUID taskId, UUID subtaskId) {
        Task subtask = findTask(subtaskId);
        if (!taskId.equals(subtask.getParentTaskId())) {
            throw new AppException(ErrorCode.TASK_NOT_FOUND, "Không tìm thấy checklist con");
        }
        return subtask;
    }

    /**
     * Kiểm tra task cha hợp lệ (cùng dự án, không phải subtask).
     *
     * @param projectId dự án
     * @param parentId  task cha
     * @return entity task cha
     * @throws AppException TASK_PARENT_INVALID
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    private Task validateParent(UUID projectId, UUID parentId) {
        Task parent = findTask(parentId);
        if (!parent.getProjectId().equals(projectId) || parent.getParentTaskId() != null) {
            throw new AppException(ErrorCode.TASK_PARENT_INVALID, "Task cha không hợp lệ");
        }
        return parent;
    }

    /**
     * Validate khoảng thời gian startDate ≤ deadline.
     *
     * @param startDate ngày bắt đầu
     * @param deadline  hạn hoàn thành
     * @throws AppException TASK_DEADLINE_INVALID
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    private void validateWindow(Instant startDate, Instant deadline) {
        if (startDate != null && deadline != null && startDate.isAfter(deadline)) {
            throw new AppException(ErrorCode.TASK_DEADLINE_INVALID,
                    "Ngày bắt đầu không được sau hạn hoàn thành");
        }
    }

    /**
     * Kiểm tra version client gửi lên có khớp không.
     *
     * @param task    task hiện tại
     * @param version version client giữ (null = bỏ qua)
     * @throws AppException TASK_VERSION_CONFLICT
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    private void checkVersion(Task task, Long version) {
        if (version != null && version != task.getVersion()) {
            throw new AppException(ErrorCode.TASK_VERSION_CONFLICT,
                    "Task đã được người khác cập nhật, vui lòng tải lại");
        }
    }

    /**
     * Áp dụng trạng thái và cập nhật {@code completedAt} tương ứng.
     *
     * @param task   task
     * @param status trạng thái mới
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    private void applyStatus(Task task, TaskStatus status) {
        task.setStatus(status);
        if (status == TaskStatus.DONE) {
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(Instant.now());
            }
        } else {
            task.setCompletedAt(null);
        }
    }

    /**
     * Tính vị trí Kanban khi tạo task.
     *
     * @param requested vị trí client gửi (null = cuối cột)
     * @param projectId dự án
     * @param status    cột
     * @param parent    task cha (null nếu task gốc)
     * @return vị trí
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    private double resolvePosition(Double requested, UUID projectId, TaskStatus status, Task parent) {
        if (requested != null) {
            return requested;
        }
        if (parent != null) {
            return taskRepository.maxSiblingPosition(parent.getId()) + 1000;
        }
        return taskRepository.maxRootPosition(projectId, status) + 1000;
    }

    /**
     * Chuẩn hoá + validate danh sách assignee phải là thành viên dự án.
     *
     * @param projectId dự án
     * @param actorId   người yêu cầu
     * @param ids       danh sách id client gửi
     * @return danh sách id duy nhất
     * @throws AppException TASK_ASSIGNEE_INVALID nếu có người ngoài dự án
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C2
     */
    private List<UUID> resolveAssignees(UUID projectId, UUID actorId, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<UUID> unique = new LinkedHashSet<>(ids);
        Set<UUID> memberIds = projectService.listMembers(projectId, actorId).stream()
                .map(ProjectMemberResponse::userId)
                .collect(Collectors.toSet());
        if (!memberIds.containsAll(unique)) {
            throw new AppException(ErrorCode.TASK_ASSIGNEE_INVALID,
                    "Người được gán phải là thành viên dự án");
        }
        return List.copyOf(unique);
    }

    /**
     * Lấy id assignee hiện tại của task.
     *
     * @param taskId id task
     * @return danh sách id
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C2
     */
    private List<UUID> currentAssigneeIds(UUID taskId) {
        return taskAssigneeRepository.findByTaskId(taskId).stream()
                .map(TaskAssignee::getUserId)
                .toList();
    }

    /**
     * Thay toàn bộ assignee của task.
     *
     * @param taskId id task
     * @param ids    danh sách id mới
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C2
     */
    private void replaceAssignees(UUID taskId, List<UUID> ids) {
        taskAssigneeRepository.deleteByTaskId(taskId);
        taskAssigneeRepository.flush();
        for (UUID userId : ids) {
            TaskAssignee assignee = new TaskAssignee();
            assignee.setTaskId(taskId);
            assignee.setUserId(userId);
            taskAssigneeRepository.save(assignee);
        }
    }

    /**
     * Thay toàn bộ nhãn của task (validate nhãn thuộc dự án).
     *
     * @param taskId    id task
     * @param projectId dự án
     * @param labelIds  danh sách nhãn mới (null = bỏ hết)
     * @throws AppException LABEL_NOT_FOUND nếu nhãn không thuộc dự án
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C1
     */
    private void replaceLabels(UUID taskId, UUID projectId, List<UUID> labelIds) {
        taskLabelRepository.deleteByTaskId(taskId);
        taskLabelRepository.flush();
        if (labelIds == null || labelIds.isEmpty()) {
            return;
        }
        Set<UUID> unique = new LinkedHashSet<>(labelIds);
        Map<UUID, Label> labels = labelRepository.findAllById(unique).stream()
                .collect(Collectors.toMap(Label::getId, Function.identity()));
        for (UUID labelId : unique) {
            Label label = labels.get(labelId);
            if (label == null || !label.getProjectId().equals(projectId)) {
                throw new AppException(ErrorCode.LABEL_NOT_FOUND,
                        "Nhãn không tồn tại trong dự án");
            }
            TaskLabel link = new TaskLabel();
            link.setTaskId(taskId);
            link.setLabelId(labelId);
            taskLabelRepository.save(link);
        }
    }

    /**
     * Gửi thông báo cho những người mới được gán (trừ người thao tác).
     *
     * @param task      task
     * @param userIds   người mới được gán
     * @param actorId   người thao tác
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C2, §E2
     */
    private void notifyAssignment(Task task, List<UUID> userIds, UUID actorId) {
        for (UUID userId : userIds) {
            if (userId.equals(actorId)) {
                continue;
            }
            notificationService.notify(userId, NotificationType.TASK_ASSIGNED,
                    "Bạn được giao một task", task.getTitle(), "TASK", task.getId(), null);
        }
    }

    /**
     * Nạp dữ liệu hiển thị cho nhiều task trong vài query (tránh N+1).
     *
     * @param tasks danh sách task
     * @return dữ liệu assignee/nhãn/số bình luận/tên người dùng
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    private ViewData loadViewData(Collection<Task> tasks) {
        List<UUID> ids = tasks.stream().map(Task::getId).toList();
        if (ids.isEmpty()) {
            return new ViewData(Map.of(), Map.of(), Map.of(), Map.of());
        }
        Map<UUID, List<TaskAssignee>> assignees = taskAssigneeRepository.findByTaskIdIn(ids).stream()
                .collect(Collectors.groupingBy(TaskAssignee::getTaskId));

        List<TaskLabel> links = taskLabelRepository.findByTaskIdIn(ids);
        Map<UUID, Label> labelById = labelRepository.findAllById(
                        links.stream().map(TaskLabel::getLabelId).distinct().toList()).stream()
                .collect(Collectors.toMap(Label::getId, Function.identity()));
        Map<UUID, List<Label>> labelsByTask = links.stream()
                .filter(link -> labelById.containsKey(link.getLabelId()))
                .collect(Collectors.groupingBy(TaskLabel::getTaskId,
                        Collectors.mapping(link -> labelById.get(link.getLabelId()), Collectors.toList())));

        Map<UUID, Long> commentCounts = taskCommentRepository.countByTaskIds(ids).stream()
                .collect(Collectors.toMap(TaskCommentRepository.TaskCommentCount::getTaskId,
                        TaskCommentRepository.TaskCommentCount::getCount));

        Set<UUID> userIds = assignees.values().stream()
                .flatMap(List::stream)
                .map(TaskAssignee::getUserId)
                .collect(Collectors.toSet());
        Map<UUID, String> names = userService.namesByIds(userIds);
        return new ViewData(assignees, labelsByTask, commentCounts, names);
    }

    /**
     * Ánh xạ task + dữ liệu phụ sang DTO.
     *
     * @param task        entity task
     * @param subtasks    checklist con
     * @param data        dữ liệu assignee/nhãn/số bình luận/tên
     * @param attachments file đính kèm
     * @return DTO task
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    private TaskResponse toResponse(Task task, List<Task> subtasks, ViewData data,
                                    List<FileResponse> attachments) {
        List<TaskResponse.AssigneeResponse> assignees = data.assignees()
                .getOrDefault(task.getId(), List.of()).stream()
                .map(assignee -> new TaskResponse.AssigneeResponse(assignee.getUserId(),
                        data.names().getOrDefault(assignee.getUserId(), "")))
                .toList();
        List<LabelResponse> labels = data.labels().getOrDefault(task.getId(), List.of()).stream()
                .map(LabelResponse::from)
                .toList();
        List<TaskResponse.SubtaskResponse> subtaskResponses = subtasks.stream()
                .map(subtask -> new TaskResponse.SubtaskResponse(subtask.getId(), subtask.getTitle(),
                        subtask.getStatus() == TaskStatus.DONE, subtask.getPosition()))
                .toList();
        return new TaskResponse(task.getId(), task.getProjectId(), task.getParentTaskId(),
                task.getTitle(), task.getDescription(), task.getStatus().name(),
                task.getPriority().name(), task.getStartDate(), task.getDeadline(),
                task.getPosition(), task.getCreatedBy(), task.getCompletedAt(), task.getVersion(),
                assignees, labels, subtaskResponses, attachments,
                data.commentCounts().getOrDefault(task.getId(), 0L),
                task.getCreatedAt(), task.getUpdatedAt());
    }

    /**
     * Dữ liệu phụ đã nạp sẵn cho danh sách task.
     *
     * @param assignees     assignee theo task
     * @param labels        nhãn theo task
     * @param commentCounts số bình luận theo task
     * @param names         tên người dùng theo id
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    private record ViewData(Map<UUID, List<TaskAssignee>> assignees,
                            Map<UUID, List<Label>> labels,
                            Map<UUID, Long> commentCounts,
                            Map<UUID, String> names) {
    }
}
