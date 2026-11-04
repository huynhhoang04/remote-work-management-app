import { useMutation, useQueryClient } from '@tanstack/react-query';
import { authApi, type LoginPayload, type RegisterPayload } from '../../api/auth';
import { getRefreshToken } from '../../api/tokenStore';
import { usersApi } from '../../api/users';
import { unregisterPushNotifications } from '../push/push';
import { useSession } from '../../store/session';

/** Đăng nhập — lưu phiên và xoá cache dữ liệu của phiên trước. */
export function useLogin() {
  const { signIn } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: LoginPayload) => authApi.login(payload),
    onSuccess: async (tokens) => {
      await signIn(tokens);
      queryClient.clear();
    },
  });
}

/** Đăng ký xong tự đăng nhập luôn (bớt 1 bước cho người dùng). */
export function useRegister() {
  const { signIn } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: RegisterPayload) => {
      await authApi.register(payload);
      return authApi.login({ email: payload.email, password: payload.password });
    },
    onSuccess: async (tokens) => {
      await signIn(tokens);
      queryClient.clear();
    },
  });
}

export function useForgotPassword() {
  return useMutation({
    mutationFn: (email: string) => authApi.forgotPassword(email),
  });
}

export function useChangePassword() {
  return useMutation({
    mutationFn: (payload: { oldPassword: string; newPassword: string }) => usersApi.changePassword(payload),
  });
}

/** Đăng xuất: gọi backend thu hồi refresh token (best effort) rồi xoá phiên + cache. */
export function useLogout() {
  const { signOut } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      await unregisterPushNotifications();
      const refreshToken = getRefreshToken();
      if (refreshToken) {
        try {
          await authApi.logout(refreshToken);
        } catch {
          // Token có thể đã hết hạn — vẫn xoá phiên local.
        }
      }
    },
    onSettled: async () => {
      await signOut();
      queryClient.clear();
    },
  });
}
