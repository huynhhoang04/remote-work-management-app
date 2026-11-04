import { useQuery } from '@tanstack/react-query';
import { statsApi } from '../../api/stats';

export function useToday() {
  return useQuery({
    queryKey: ['today'],
    queryFn: statsApi.today,
    staleTime: 30_000,
  });
}

export function useMyStats(from: string, to: string) {
  return useQuery({
    queryKey: ['me', 'stats', from, to],
    queryFn: () => statsApi.myStats(from, to),
    enabled: Boolean(from && to),
  });
}

export function useProjectStats(projectId: string) {
  return useQuery({
    queryKey: ['projectStats', projectId],
    queryFn: () => statsApi.projectStats(projectId),
    enabled: Boolean(projectId),
  });
}
