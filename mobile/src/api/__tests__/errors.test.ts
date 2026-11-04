import { ApiError, normalizeError } from '../errors';

/** Giả lập AxiosError để không cần gọi mạng trong unit test. */
function axiosLike(partial: Record<string, unknown>) {
  return { isAxiosError: true, ...partial };
}

describe('normalizeError', () => {
  test('giữ code + message từ backend', () => {
    const error = normalizeError(
      axiosLike({
        response: { status: 409, data: { code: 'USER_EMAIL_EXISTS', message: 'Email đã được sử dụng' } },
      }),
    );

    expect(error).toBeInstanceOf(ApiError);
    expect(error.code).toBe('USER_EMAIL_EXISTS');
    expect(error.message).toBe('Email đã được sử dụng');
    expect(error.status).toBe(409);
  });

  test('timeout -> NETWORK_TIMEOUT', () => {
    const error = normalizeError(axiosLike({ code: 'ECONNABORTED' }));
    expect(error.code).toBe('NETWORK_TIMEOUT');
  });

  test('lỗi mạng không có response -> NETWORK_ERROR', () => {
    const error = normalizeError(axiosLike({}));
    expect(error.code).toBe('NETWORK_ERROR');
  });

  test('lỗi lạ -> UNKNOWN', () => {
    const error = normalizeError(new Error('boom'));
    expect(error.code).toBe('UNKNOWN');
  });

  test('ApiError đi qua không bị đổi', () => {
    const original = new ApiError('PROJECT_FORBIDDEN', 'Forbidden', 403);
    expect(normalizeError(original)).toBe(original);
  });
});
