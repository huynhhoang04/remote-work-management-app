import type { NotificationDto, PageDto, UnreadCountDto } from '../types/api';
import { api } from './client';

/** API trung tâm thông báo — docs/plan-backend/02-danh-sach-api.md §9. */
export const notificationsApi = {
  list(page: number, size: number): Promise<PageDto<NotificationDto>> {
    return api
      .get<PageDto<NotificationDto>>('/notifications', { params: { page, size } })
      .then((response) => response.data);
  },

  unreadCount(): Promise<UnreadCountDto> {
    return api
      .get<UnreadCountDto>('/notifications/unread-count')
      .then((response) => response.data);
  },

  markRead(notificationId: string): Promise<void> {
    return api.patch<void>(`/notifications/${notificationId}/read`).then(() => undefined);
  },

  markAllRead(): Promise<void> {
    return api.patch<void>('/notifications/read-all').then(() => undefined);
  },
};
