import axios from 'axios';
import type { ApiErrorBody } from '../types/api';
import { STR } from '../utils/constants';

/**
 * Lỗi API chuẩn hoá: code backend (ErrorCode) + message tiếng Việt.
 * @reference docs/plan-app/01-phase-1.md §6
 */
export class ApiError extends Error {
  readonly code: string;
  readonly status?: number;

  constructor(code: string, message: string, status?: number) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

/** Đổi mọi lỗi (axios/network/unknown) thành ApiError để UI hiển thị thống nhất. */
export function normalizeError(error: unknown): ApiError {
  if (isApiError(error)) return error;

  if (axios.isAxiosError(error)) {
    const body = error.response?.data as ApiErrorBody | undefined;
    if (body?.code) {
      return new ApiError(body.code, body.message ?? STR.unknownError, error.response?.status);
    }
    if (error.code === 'ECONNABORTED') {
      return new ApiError('NETWORK_TIMEOUT', STR.timeoutError);
    }
    if (!error.response) {
      return new ApiError('NETWORK_ERROR', STR.networkError);
    }
    return new ApiError('HTTP_ERROR', STR.unknownError, error.response.status);
  }

  return new ApiError('UNKNOWN', STR.unknownError);
}
