package com.example.remoteapp.task;

import com.example.remoteapp.common.SecurityUtils;
import com.example.remoteapp.file.dto.FileResponse;
import com.example.remoteapp.task.dto.AssigneesRequest;
import com.example.remoteapp.task.dto.CommentRequest;
import com.example.remoteapp.task.dto.CommentResponse;
import com.example.remoteapp.task.dto.LabelRequest;
import com.example.remoteapp.task.dto.LabelResponse;
import com.example.remoteapp.task.dto.PositionRequest;
import com.example.remoteapp.task.dto.StatusRequest;
import com.example.remoteapp.task.dto.SubtaskRequest;
import com.example.remoteapp.task.dto.SubtaskUpdateRequest;
import com.example.remoteapp.task.dto.TaskRequest;
import com.example.remoteapp.task.dto.TaskResponse;
import com.example.remoteapp.task.dto.TaskUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller task, nhãn, checklist, bình luận, file đính kèm (plan 02 §5).
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §5
 */
@RestController
@RequestMapping("/api/v1")
public class TaskController {

    private final TaskService taskService;
    private final TaskCommentService commentService;

    /**
     * Khởi tạo controller (constructor injection tường minh).
     *
     * @param taskService    nghiệp vụ task
     * @param commentService nghiệp vụ bình luận
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public TaskController(TaskService taskService, TaskCommentService commentService) {
        this.taskService = taskService;
        this.commentService = commentService;
    }

    /**
     * Danh sách toàn bộ task gốc của dự án (client sort/filter).
     *
     * @param id id dự án
     * @return danh sách task
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @GetMapping("/projects/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> list(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.list(id, SecurityUtils.currentUserId()));
    }

    /**
     * Tạo task mới.
     *
     * @param request dữ liệu task
     * @return 201 + task
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @PostMapping("/tasks")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.create(SecurityUtils.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + response.id())).body(response);
    }

    /**
     * Xem chi tiết task.
     *
     * @param id id task
     * @return task
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.get(id, SecurityUtils.currentUserId()));
    }

    /**
     * Cập nhật task (PATCH).
     *
     * @param id      id task
     * @param request trường cần sửa
     * @return task sau cập nhật
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @PatchMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody TaskUpdateRequest request) {
        return ResponseEntity.ok(taskService.update(id, SecurityUtils.currentUserId(), request));
    }

    /**
     * Xoá task (soft delete).
     *
     * @param id id task
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        taskService.delete(id, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Đổi trạng thái task.
     *
     * @param id      id task
     * @param request trạng thái mới
     * @return task sau cập nhật
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @PatchMapping("/tasks/{id}/status")
    public ResponseEntity<TaskResponse> changeStatus(@PathVariable UUID id,
                                                     @Valid @RequestBody StatusRequest request) {
        return ResponseEntity.ok(taskService.changeStatus(id, SecurityUtils.currentUserId(),
                request.status()));
    }

    /**
     * Cập nhật vị trí Kanban (kéo thả).
     *
     * @param id      id task
     * @param request cột + vị trí
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @PatchMapping("/tasks/{id}/position")
    public ResponseEntity<Void> changePosition(@PathVariable UUID id,
                                               @Valid @RequestBody PositionRequest request) {
        taskService.changePosition(id, SecurityUtils.currentUserId(), request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gán lại người thực hiện (danh sách đầy đủ).
     *
     * @param id      id task
     * @param request danh sách assignee
     * @return task sau cập nhật
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @PatchMapping("/tasks/{id}/assignees")
    public ResponseEntity<TaskResponse> setAssignees(@PathVariable UUID id,
                                                     @Valid @RequestBody AssigneesRequest request) {
        return ResponseEntity.ok(taskService.setAssignees(id, SecurityUtils.currentUserId(), request));
    }

    /**
     * Danh sách nhãn của dự án.
     *
     * @param id id dự án
     * @return danh sách nhãn
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @GetMapping("/projects/{id}/labels")
    public ResponseEntity<List<LabelResponse>> listLabels(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.listLabels(id, SecurityUtils.currentUserId()));
    }

    /**
     * Tạo nhãn trong dự án.
     *
     * @param id      id dự án
     * @param request tên + màu
     * @return 201 + nhãn
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @PostMapping("/projects/{id}/labels")
    public ResponseEntity<LabelResponse> createLabel(@PathVariable UUID id,
                                                     @Valid @RequestBody LabelRequest request) {
        LabelResponse response = taskService.createLabel(id, SecurityUtils.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/v1/labels/" + response.id())).body(response);
    }

    /**
     * Xoá nhãn khỏi dự án.
     *
     * @param id id nhãn
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @DeleteMapping("/labels/{id}")
    public ResponseEntity<Void> deleteLabel(@PathVariable UUID id) {
        taskService.deleteLabel(id, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Thêm checklist con.
     *
     * @param id      id task cha
     * @param request tiêu đề subtask
     * @return 201 + subtask
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @PostMapping("/tasks/{id}/subtasks")
    public ResponseEntity<TaskResponse> addSubtask(@PathVariable UUID id,
                                                   @Valid @RequestBody SubtaskRequest request) {
        TaskResponse response = taskService.addSubtask(id, SecurityUtils.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + response.id())).body(response);
    }

    /**
     * Cập nhật checklist con.
     *
     * @param id      id task cha
     * @param sid     id subtask
     * @param request trường cần sửa
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @PatchMapping("/tasks/{id}/subtasks/{sid}")
    public ResponseEntity<Void> updateSubtask(@PathVariable UUID id, @PathVariable UUID sid,
                                              @Valid @RequestBody SubtaskUpdateRequest request) {
        taskService.updateSubtask(id, sid, SecurityUtils.currentUserId(), request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Xoá checklist con.
     *
     * @param id  id task cha
     * @param sid id subtask
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @DeleteMapping("/tasks/{id}/subtasks/{sid}")
    public ResponseEntity<Void> deleteSubtask(@PathVariable UUID id, @PathVariable UUID sid) {
        taskService.deleteSubtask(id, sid, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Danh sách bình luận của task.
     *
     * @param id id task
     * @return danh sách bình luận
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @GetMapping("/tasks/{id}/comments")
    public ResponseEntity<List<CommentResponse>> listComments(@PathVariable UUID id) {
        return ResponseEntity.ok(commentService.list(id, SecurityUtils.currentUserId()));
    }

    /**
     * Thêm bình luận (kèm mention).
     *
     * @param id      id task
     * @param request nội dung + mention
     * @return 201 + bình luận
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @PostMapping("/tasks/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable UUID id,
                                                      @Valid @RequestBody CommentRequest request) {
        CommentResponse response = commentService.add(id, SecurityUtils.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + id + "/comments/" + response.id()))
                .body(response);
    }

    /**
     * Sửa bình luận (chỉ tác giả).
     *
     * @param id      id task
     * @param cid     id bình luận
     * @param request nội dung mới
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @PatchMapping("/tasks/{id}/comments/{cid}")
    public ResponseEntity<Void> updateComment(@PathVariable UUID id, @PathVariable UUID cid,
                                              @Valid @RequestBody CommentRequest request) {
        commentService.update(id, cid, SecurityUtils.currentUserId(), request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Xoá bình luận (chỉ tác giả).
     *
     * @param id  id task
     * @param cid id bình luận
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @DeleteMapping("/tasks/{id}/comments/{cid}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID id, @PathVariable UUID cid) {
        commentService.delete(id, cid, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Upload file đính kèm cho task.
     *
     * @param id   id task
     * @param file file multipart
     * @return 201 + metadata file
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @PostMapping(value = "/tasks/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> uploadAttachment(@PathVariable UUID id,
                                                         @RequestPart("file") MultipartFile file) {
        FileResponse response = taskService.addAttachment(id, SecurityUtils.currentUserId(), file);
        return ResponseEntity.created(URI.create("/api/v1/files/" + response.id())).body(response);
    }

    /**
     * Xoá file đính kèm của task.
     *
     * @param id  id task
     * @param fid id file
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    @DeleteMapping("/tasks/{id}/attachments/{fid}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable UUID id, @PathVariable UUID fid) {
        taskService.deleteAttachment(id, fid, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
