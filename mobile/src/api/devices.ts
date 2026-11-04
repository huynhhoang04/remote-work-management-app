import { api } from './client';

/** API thiết bị FCM — docs/plan-backend/02-danh-sach-api.md §3. */
export const devicesApi = {
  register(fcmToken: string, platform: string): Promise<void> {
    return api.post<void>('/devices/token', { fcmToken, platform }).then(() => undefined);
  },

  unregister(fcmToken: string): Promise<void> {
    return api.delete<void>('/devices/token', { data: { fcmToken } }).then(() => undefined);
  },
};
