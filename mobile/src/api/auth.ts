import type { TokenResponse, UserDto } from '../types/api';
import { api } from './client';

export interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

/** API xác thực — docs/plan-backend/02-danh-sach-api.md §2. */
export const authApi = {
  register(payload: RegisterPayload): Promise<UserDto> {
    return api.post<UserDto>('/auth/register', payload).then((response) => response.data);
  },

  login(payload: LoginPayload): Promise<TokenResponse> {
    return api.post<TokenResponse>('/auth/login', payload).then((response) => response.data);
  },

  logout(refreshToken: string): Promise<void> {
    return api.post<void>('/auth/logout', { refreshToken }).then(() => undefined);
  },

  forgotPassword(email: string): Promise<void> {
    return api.post<void>('/auth/forgot-password', { email }).then(() => undefined);
  },
};
