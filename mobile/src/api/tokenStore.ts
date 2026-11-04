import * as SecureStore from 'expo-secure-store';

const ACCESS_TOKEN_KEY = 'remoteapp.accessToken';
const REFRESH_TOKEN_KEY = 'remoteapp.refreshToken';
const USER_KEY = 'remoteapp.user';

/** Cache đồng bộ cho interceptor (SecureStore chỉ có API async). */
let accessToken: string | null = null;
let refreshToken: string | null = null;

/** Đọc token từ SecureStore vào cache — gọi 1 lần khi app khởi động. */
export async function loadTokens(): Promise<void> {
  accessToken = await SecureStore.getItemAsync(ACCESS_TOKEN_KEY);
  refreshToken = await SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
}

export function getAccessToken(): string | null {
  return accessToken;
}

export function getRefreshToken(): string | null {
  return refreshToken;
}

/** Lưu cặp token (sau login/refresh). */
export async function setTokens(nextAccessToken: string, nextRefreshToken: string): Promise<void> {
  accessToken = nextAccessToken;
  refreshToken = nextRefreshToken;
  await SecureStore.setItemAsync(ACCESS_TOKEN_KEY, nextAccessToken);
  await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, nextRefreshToken);
}

/** Xoá token khỏi bộ nhớ + SecureStore (logout/hết phiên). */
export async function clearTokens(): Promise<void> {
  accessToken = null;
  refreshToken = null;
  await SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY);
  await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
}

/** Lưu/đọc user hiện tại (không nhạy cảm nhưng để hiển thị ngay khi mở app). */
export async function saveUser(userJson: string): Promise<void> {
  await SecureStore.setItemAsync(USER_KEY, userJson);
}

export async function loadUser(): Promise<string | null> {
  return SecureStore.getItemAsync(USER_KEY);
}

export async function clearUser(): Promise<void> {
  await SecureStore.deleteItemAsync(USER_KEY);
}
