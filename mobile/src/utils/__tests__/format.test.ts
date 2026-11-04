import { formatBytes, formatRelative, isOverdue } from '../format';

describe('format helpers', () => {
  test('formatBytes', () => {
    expect(formatBytes(0)).toBe('0 B');
    expect(formatBytes(512)).toBe('512 B');
    expect(formatBytes(1536)).toBe('1.5 KB');
    expect(formatBytes(5 * 1024 * 1024)).toBe('5.0 MB');
  });

  test('isOverdue', () => {
    expect(isOverdue(undefined)).toBe(false);
    expect(isOverdue('2000-01-01T00:00:00.000Z')).toBe(true);
    expect(isOverdue('2999-01-01T00:00:00.000Z')).toBe(false);
  });

  test('formatRelative trả về Vừa xong cho thời điểm hiện tại', () => {
    expect(formatRelative(new Date().toISOString())).toBe('Vừa xong');
  });
});
