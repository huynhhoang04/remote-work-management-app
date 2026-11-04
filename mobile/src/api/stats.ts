import type { MyStatsDto, ProjectStatsDto, TodayDto } from '../types/api';
import { api } from './client';

/** API thống kê — docs/plan-backend/02-danh-sach-api.md §7. */
export const statsApi = {
  today(): Promise<TodayDto> {
    return api.get<TodayDto>('/me/today').then((response) => response.data);
  },

  myStats(from: string, to: string): Promise<MyStatsDto> {
    return api
      .get<MyStatsDto>('/me/stats', { params: { from, to } })
      .then((response) => response.data);
  },

  projectStats(projectId: string): Promise<ProjectStatsDto> {
    return api
      .get<ProjectStatsDto>(`/projects/${projectId}/stats`)
      .then((response) => response.data);
  },
};
