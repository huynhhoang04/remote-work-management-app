import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { projectsApi, type CreateProjectPayload } from '../../api/projects';
import type { AuditLogDto, PageDto, ProjectRole } from '../../types/api';

export function useProjects() {
  return useQuery({ queryKey: ['projects'], queryFn: projectsApi.list });
}

export function useProject(projectId: string) {
  return useQuery({
    queryKey: ['projects', projectId],
    queryFn: () => projectsApi.get(projectId),
    enabled: Boolean(projectId),
  });
}

export function useMembers(projectId: string) {
  return useQuery({
    queryKey: ['projects', projectId, 'members'],
    queryFn: () => projectsApi.members(projectId),
    enabled: Boolean(projectId),
  });
}

export function useInvites(projectId: string, enabled: boolean) {
  return useQuery({
    queryKey: ['projects', projectId, 'invites'],
    queryFn: () => projectsApi.listInvites(projectId),
    enabled: Boolean(projectId) && enabled,
  });
}

export function useCreateProject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateProjectPayload) => projectsApi.create(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects'] });
    },
  });
}

export function useUpdateProject(projectId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateProjectPayload) => projectsApi.update(projectId, payload),
    onSuccess: async (project) => {
      queryClient.setQueryData(['projects', projectId], project);
      await queryClient.invalidateQueries({ queryKey: ['projects'] });
    },
  });
}

export function useDeleteProject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (projectId: string) => projectsApi.remove(projectId),
    onSuccess: async (_data, projectId) => {
      queryClient.removeQueries({ queryKey: ['projects', projectId] });
      await queryClient.invalidateQueries({ queryKey: ['projects'] });
    },
  });
}

export function useJoinProject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (code: string) => projectsApi.join(code),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects'] });
    },
  });
}

export function useCreateInvite(projectId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (expiresAt?: string | null) => projectsApi.createInvite(projectId, expiresAt),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'invites'] });
    },
  });
}

export function useRevokeInvite(projectId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (inviteId: string) => projectsApi.revokeInvite(projectId, inviteId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'invites'] });
    },
  });
}

export function useChangeMemberRole(projectId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: ProjectRole }) =>
      projectsApi.changeMemberRole(projectId, userId, role),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'members'] });
    },
  });
}

export function useRemoveMember(projectId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => projectsApi.removeMember(projectId, userId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'members'] });
    },
  });
}

export function useAuditLogs(projectId: string, action: string) {
  return useInfiniteQuery({
    queryKey: ['projects', projectId, 'audit', action],
    queryFn: ({ pageParam }) => projectsApi.auditLogs(projectId, pageParam, 20, action),
    initialPageParam: 0,
    getNextPageParam: (lastPage: PageDto<AuditLogDto>) =>
      lastPage.number + 1 < lastPage.totalPages ? lastPage.number + 1 : undefined,
    enabled: Boolean(projectId),
  });
}
