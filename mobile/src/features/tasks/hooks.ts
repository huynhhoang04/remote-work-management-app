import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { CreateTaskPayload, TaskDto, UpdateTaskPayload } from '../../types/api';
import { tasksApi } from '../../api/tasks';

/** Làm mới mọi cache phụ thuộc task (danh sách, today, thống kê dự án). */
function invalidateTaskSideEffects(
  queryClient: ReturnType<typeof useQueryClient>,
  projectId?: string | null,
): Promise<unknown> {
  const jobs: Promise<unknown>[] = [
    queryClient.invalidateQueries({ queryKey: ['tasks'] }),
    queryClient.invalidateQueries({ queryKey: ['today'] }),
  ];
  if (projectId) {
    jobs.push(queryClient.invalidateQueries({ queryKey: ['projectStats', projectId] }));
  }
  return Promise.all(jobs);
}

export function useProjectTasks(projectId: string) {
  return useQuery({
    queryKey: ['tasks', projectId],
    queryFn: () => tasksApi.list(projectId),
    enabled: Boolean(projectId),
  });
}

export function useTask(taskId: string) {
  return useQuery({
    queryKey: ['task', taskId],
    queryFn: () => tasksApi.get(taskId),
    enabled: Boolean(taskId),
  });
}

export function useCreateTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateTaskPayload) => tasksApi.create(payload),
    onSuccess: (task) => {
      queryClient.setQueryData(['task', task.id], task);
      void invalidateTaskSideEffects(queryClient, task.projectId);
    },
  });
}

export function useUpdateTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, payload }: { taskId: string; payload: UpdateTaskPayload }) =>
      tasksApi.update(taskId, payload),
    onSuccess: (task) => {
      queryClient.setQueryData(['task', task.id], task);
      void invalidateTaskSideEffects(queryClient, task.projectId);
    },
  });
}

export function useDeleteTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (taskId: string) => tasksApi.remove(taskId),
    onSuccess: (_data, taskId) => {
      queryClient.removeQueries({ queryKey: ['task', taskId] });
      void invalidateTaskSideEffects(queryClient);
    },
  });
}

export function useChangeTaskStatus(projectId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, status }: { taskId: string; status: TaskDto['status'] }) =>
      tasksApi.changeStatus(taskId, status),
    onSuccess: (task) => {
      queryClient.setQueryData(['task', task.id], task);
      void invalidateTaskSideEffects(queryClient, projectId ?? task.projectId);
    },
  });
}

export function useChangeTaskPosition(projectId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      taskId,
      status,
      position,
    }: {
      taskId: string;
      status: TaskDto['status'];
      position: number;
    }) => tasksApi.changePosition(taskId, status, position),
    onSuccess: () => {
      void invalidateTaskSideEffects(queryClient, projectId);
    },
  });
}

export function useSetTaskAssignees(projectId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, assigneeIds }: { taskId: string; assigneeIds: string[] }) =>
      tasksApi.setAssignees(taskId, assigneeIds),
    onSuccess: (task) => {
      queryClient.setQueryData(['task', task.id], task);
      void invalidateTaskSideEffects(queryClient, projectId ?? task.projectId);
    },
  });
}

// ---------------------------------------------------------------------------
// Labels
// ---------------------------------------------------------------------------

export function useLabels(projectId: string) {
  return useQuery({
    queryKey: ['labels', projectId],
    queryFn: () => tasksApi.listLabels(projectId),
    enabled: Boolean(projectId),
  });
}

export function useCreateLabel(projectId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { name: string; color: string }) => tasksApi.createLabel(projectId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['labels', projectId] });
    },
  });
}

export function useDeleteLabel(projectId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (labelId: string) => tasksApi.deleteLabel(labelId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['labels', projectId] }),
        queryClient.invalidateQueries({ queryKey: ['tasks', projectId] }),
      ]);
    },
  });
}

// ---------------------------------------------------------------------------
// Subtasks / comments / attachments
// ---------------------------------------------------------------------------

export function useAddSubtask(taskId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (title: string) => tasksApi.addSubtask(taskId, title),
    onSuccess: async (subtask) => {
      await queryClient.invalidateQueries({ queryKey: ['task', taskId], exact: true });
      await invalidateTaskSideEffects(queryClient, subtask.projectId);
    },
  });
}

export function useUpdateSubtask(taskId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      subtaskId,
      payload,
    }: {
      subtaskId: string;
      payload: { title?: string; done?: boolean };
    }) => tasksApi.updateSubtask(taskId, subtaskId, payload),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['task', taskId] }),
        queryClient.invalidateQueries({ queryKey: ['tasks'] }),
      ]);
    },
  });
}

export function useDeleteSubtask(taskId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (subtaskId: string) => tasksApi.deleteSubtask(taskId, subtaskId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['task', taskId] });
    },
  });
}

export function useComments(taskId: string) {
  return useQuery({
    queryKey: ['comments', taskId],
    queryFn: () => tasksApi.listComments(taskId),
    enabled: Boolean(taskId),
  });
}

export function useAddComment(taskId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ content, mentionUserIds }: { content: string; mentionUserIds: string[] }) =>
      tasksApi.addComment(taskId, content, mentionUserIds),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['comments', taskId] }),
        queryClient.invalidateQueries({ queryKey: ['task', taskId] }),
      ]);
    },
  });
}

export function useDeleteComment(taskId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (commentId: string) => tasksApi.deleteComment(taskId, commentId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['comments', taskId] }),
        queryClient.invalidateQueries({ queryKey: ['task', taskId] }),
      ]);
    },
  });
}

export function useUploadAttachment(taskId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: Parameters<typeof tasksApi.uploadAttachment>[1]) =>
      tasksApi.uploadAttachment(taskId, file),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['task', taskId] });
    },
  });
}

export function useDeleteAttachment(taskId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (fileId: string) => tasksApi.deleteAttachment(taskId, fileId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['task', taskId] });
    },
  });
}
