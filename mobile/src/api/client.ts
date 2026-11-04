import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { TokenResponse } from '../types/api';
import { beginRequest, endRequest } from './activity';
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './tokenStore';

/**
 * Axios client duy nhất cho toàn app: gắn Bearer token + tự refresh (single-flight).
 * @reference docs/plan-app/01-phase-1.md §6
 */
export const API_BASE_URL = (process.env.EXPO_PUBLIC_API_URL ?? 'https://remoteapp-backend.onrender.com').replace(
  /\/+$/,
  '',
);

const TIMEOUT_MS = 60000;

export const api = axios.create({
  baseURL: `${API_BASE_URL}/api/v1`,
  timeout: TIMEOUT_MS,
  headers: { 'Content-Type': 'application/json' },
});

/** Instance "sạch" cho refresh token — không đi qua interceptor (tránh vòng lặp 401). */
const rawApi = axios.create({
  baseURL: `${API_BASE_URL}/api/v1`,
  timeout: TIMEOUT_MS,
  headers: { 'Content-Type': 'application/json' },
});

/** Các endpoint auth không cần (và không nên) gắn Bearer token. */
const PUBLIC_AUTH_URLS = ['/auth/register', '/auth/login', '/auth/refresh', '/auth/forgot-password', '/auth/reset-password'];

function isPublicAuthUrl(url?: string): boolean {
  if (!url) return false;
  return PUBLIC_AUTH_URLS.some((publicUrl) => url.startsWith(publicUrl));
}

let onSessionExpired: (() => void) | null = null;

/** SessionProvider đăng ký: refresh thất bại → xoá phiên + điều hướng Login. */
export function setSessionExpiredHandler(handler: (() => void) | null): void {
  onSessionExpired = handler;
}

let refreshPromise: Promise<string | null> | null = null;

/** Refresh token single-flight: nhiều request 401 cùng lúc chỉ gọi refresh 1 lần. */
async function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const token = getRefreshToken();
      if (!token) return null;
      try {
        const { data } = await rawApi.post<TokenResponse>('/auth/refresh', { refreshToken: token });
        await setTokens(data.accessToken, data.refreshToken);
        return data.accessToken;
      } catch {
        await clearTokens();
        return null;
      }
    })();
  }

  try {
    return await refreshPromise;
  } finally {
    refreshPromise = null;
  }
}

api.interceptors.request.use((config) => {
  beginRequest();
  const token = getAccessToken();
  if (token && !isPublicAuthUrl(config.url)) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

api.interceptors.response.use(
  (response) => {
    endRequest();
    return response;
  },
  async (error: AxiosError) => {
    endRequest();
    const config = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;
    const isUnauthorized = error.response?.status === 401;

    if (isUnauthorized && config && !config._retried && !isPublicAuthUrl(config.url)) {
      config._retried = true;
      const newToken = await refreshAccessToken();
      if (newToken) {
        config.headers.set('Authorization', `Bearer ${newToken}`);
        return api.request(config);
      }
      onSessionExpired?.();
    }

    return Promise.reject(error);
  },
);
