import { QueryClient } from '@tanstack/react-query';

/**
 * QueryClient dùng chung: retry có backoff cho Render free cold start.
 * @reference docs/plan-app/01-phase-1.md §6
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 2,
      retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 8000),
      staleTime: 30_000,
      refetchOnWindowFocus: true,
    },
    mutations: {
      retry: 0,
    },
  },
});
