import type {
  AuditLogDto,
  InviteDto,
  MemberDto,
  PageDto,
  ProjectDto,
  ProjectRole,
} from '../types/api';
import { api } from './client';

export interface CreateProjectPayload {
  name: string;
  description?: string;
}

/** API dự án / thành viên / link mời / audit — docs/plan-backend/02-danh-sach-api.md §4. */
export const projectsApi = {
  list(): Promise<ProjectDto[]> {
    return api.get<ProjectDto[]>('/projects').then((response) => response.data);
  },

  create(payload: CreateProjectPayload): Promise<ProjectDto> {
    return api.post<ProjectDto>('/projects', payload).then((response) => response.data);
  },

  get(projectId: string): Promise<ProjectDto> {
    return api.get<ProjectDto>(`/projects/${projectId}`).then((response) => response.data);
  },

  update(projectId: string, payload: CreateProjectPayload): Promise<ProjectDto> {
    return api.patch<ProjectDto>(`/projects/${projectId}`, payload).then((response) => response.data);
  },

  remove(projectId: string): Promise<void> {
    return api.delete<void>(`/projects/${projectId}`).then(() => undefined);
  },

  members(projectId: string): Promise<MemberDto[]> {
    return api.get<MemberDto[]>(`/projects/${projectId}/members`).then((response) => response.data);
  },

  changeMemberRole(projectId: string, userId: string, role: ProjectRole): Promise<void> {
    return api
      .patch<void>(`/projects/${projectId}/members/${userId}`, { role })
      .then(() => undefined);
  },

  removeMember(projectId: string, userId: string): Promise<void> {
    return api.delete<void>(`/projects/${projectId}/members/${userId}`).then(() => undefined);
  },

  createInvite(projectId: string, expiresAt?: string | null): Promise<InviteDto> {
    return api
      .post<InviteDto>(`/projects/${projectId}/invites`, expiresAt ? { expiresAt } : {})
      .then((response) => response.data);
  },

  listInvites(projectId: string): Promise<InviteDto[]> {
    return api.get<InviteDto[]>(`/projects/${projectId}/invites`).then((response) => response.data);
  },

  revokeInvite(projectId: string, inviteId: string): Promise<void> {
    return api.delete<void>(`/projects/${projectId}/invites/${inviteId}`).then(() => undefined);
  },

  join(code: string): Promise<ProjectDto> {
    return api.post<ProjectDto>('/projects/join', { code }).then((response) => response.data);
  },

  auditLogs(
    projectId: string,
    page: number,
    size: number,
    action?: string,
  ): Promise<PageDto<AuditLogDto>> {
    return api
      .get<PageDto<AuditLogDto>>(`/projects/${projectId}/audit-logs`, {
        params: { page, size, action: action || undefined },
      })
      .then((response) => response.data);
  },
};
