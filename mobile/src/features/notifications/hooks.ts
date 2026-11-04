import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { NotificationDto, PageDto } from '../../types/api';
import { notificationsApi } from '../../api/notifications';

const PAGE_SIZE = 20;

export function useNotifications() {
  return useInfiniteQuery({
    queryKey: ['notifications'],
    queryFn: ({ pageParam }) => notificationsApi.list(pageParam, PAGE_SIZE),
    initialPageParam: 0,
    getNextPageParam: (lastPage: PageDto<NotificationDto>) =>
      lastPage.number + 1 < lastPage.totalPages ? lastPage.number + 1 : undefined,
  });
}

export function useUnreadNotificationCount() {
  return useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: notificationsApi.unreadCount,
    staleTime: 30_000,
    refetchOnWindowFocus: true,
  });
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (notificationId: string) => notificationsApi.markRead(notificationId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['notifications'] }),
        queryClient.invalidateQueries({ queryKey: ['notifications', 'unread'] }),
      ]);
    },
  });
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: notificationsApi.markAllRead,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['notifications'] }),
        queryClient.invalidateQueries({ queryKey: ['notifications', 'unread'] }),
      ]);
    },
  });
}
