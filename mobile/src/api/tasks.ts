import type {
  CommentDto,
  CreateTaskPayload,
  FileDto,
  LabelDto,
  SubtaskDto,
  TaskDto,
  TaskStatus,
  UpdateTaskPayload,
} from '../types/api';
import { api } from './client';
import { uploadMultipart, type UploadableFile } from './files';

/** API task/nhãn/checklist/comment/đính kèm — docs/plan-backend/02-danh-sach-api.md §5. */
export const tasksApi = {
  list(projectId: string): Promise<TaskDto[]> {
    return api.get<TaskDto[]>(`/projects/${projectId}/tasks`).then((response) => response.data);
  },

  get(taskId: string): Promise<TaskDto> {
    return api.get<TaskDto>(`/tasks/${taskId}`).then((response) => response.data);
  },

  create(payload: CreateTaskPayload): Promise<TaskDto> {
    return api.post<TaskDto>('/tasks', payload).then((response) => response.data);
  },

  update(taskId: string, payload: UpdateTaskPayload): Promise<TaskDto> {
    return api.patch<TaskDto>(`/tasks/${taskId}`, payload).then((response) => response.data);
  },

  remove(taskId: string): Promise<void> {
    return api.delete<void>(`/tasks/${taskId}`).then(() => undefined);
  },

  changeStatus(taskId: string, status: TaskStatus): Promise<TaskDto> {
    return api.patch<TaskDto>(`/tasks/${taskId}/status`, { status }).then((response) => response.data);
  },

  changePosition(taskId: string, status: TaskStatus, position: number): Promise<void> {
    return api.patch<void>(`/tasks/${taskId}/position`, { status, position }).then(() => undefined);
  },

  setAssignees(taskId: string, assigneeIds: string[]): Promise<TaskDto> {
    return api
      .patch<TaskDto>(`/tasks/${taskId}/assignees`, { assigneeIds })
      .then((response) => response.data);
  },

  listLabels(projectId: string): Promise<LabelDto[]> {
    return api.get<LabelDto[]>(`/projects/${projectId}/labels`).then((response) => response.data);
  },

  createLabel(projectId: string, payload: { name: string; color: string }): Promise<LabelDto> {
    return api.post<LabelDto>(`/projects/${projectId}/labels`, payload).then((response) => response.data);
  },

  deleteLabel(labelId: string): Promise<void> {
    return api.delete<void>(`/labels/${labelId}`).then(() => undefined);
  },

  addSubtask(taskId: string, title: string): Promise<TaskDto> {
    return api.post<TaskDto>(`/tasks/${taskId}/subtasks`, { title }).then((response) => response.data);
  },

  updateSubtask(taskId: string, subtaskId: string, payload: { title?: string; done?: boolean }): Promise<void> {
    return api.patch<void>(`/tasks/${taskId}/subtasks/${subtaskId}`, payload).then(() => undefined);
  },

  deleteSubtask(taskId: string, subtaskId: string): Promise<void> {
    return api.delete<void>(`/tasks/${taskId}/subtasks/${subtaskId}`).then(() => undefined);
  },

  listComments(taskId: string): Promise<CommentDto[]> {
    return api.get<CommentDto[]>(`/tasks/${taskId}/comments`).then((response) => response.data);
  },

  addComment(taskId: string, content: string, mentionUserIds: string[]): Promise<CommentDto> {
    return api
      .post<CommentDto>(`/tasks/${taskId}/comments`, { content, mentionUserIds })
      .then((response) => response.data);
  },

  updateComment(taskId: string, commentId: string, content: string): Promise<void> {
    return api.patch<void>(`/tasks/${taskId}/comments/${commentId}`, { content }).then(() => undefined);
  },

  deleteComment(taskId: string, commentId: string): Promise<void> {
    return api.delete<void>(`/tasks/${taskId}/comments/${commentId}`).then(() => undefined);
  },

  uploadAttachment(taskId: string, file: UploadableFile): Promise<FileDto> {
    return uploadMultipart(`/tasks/${taskId}/attachments`, file);
  },

  deleteAttachment(taskId: string, fileId: string): Promise<void> {
    return api.delete<void>(`/tasks/${taskId}/attachments/${fileId}`).then(() => undefined);
  },
};

export type { SubtaskDto };
