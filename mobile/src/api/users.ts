import type { UserDto } from '../types/api';
import { api } from './client';

/** API hồ sơ người dùng — docs/plan-backend/02-danh-sach-api.md §3. */
export const usersApi = {
  me(): Promise<UserDto> {
    return api.get<UserDto>('/users/me').then((response) => response.data);
  },

  updateMe(payload: { fullName?: string; avatarUrl?: string }): Promise<UserDto> {
    return api.patch<UserDto>('/users/me', payload).then((response) => response.data);
  },

  changePassword(payload: { oldPassword: string; newPassword: string }): Promise<void> {
    return api.post<void>('/users/me/password', payload).then(() => undefined);
  },
};
