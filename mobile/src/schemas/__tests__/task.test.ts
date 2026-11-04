import { taskSchema } from '../task';

const base = {
  title: 'Viết báo cáo',
  description: 'Mô tả',
  status: 'TODO' as const,
  priority: 'MEDIUM' as const,
};

describe('taskSchema', () => {
  test('hợp lệ với dữ liệu tối thiểu', () => {
    expect(taskSchema.safeParse(base).success).toBe(true);
  });

  test('tiêu đề rỗng bị từ chối', () => {
    const result = taskSchema.safeParse({ ...base, title: '' });
    expect(result.success).toBe(false);
  });

  test('tiêu đề quá 200 ký tự bị từ chối', () => {
    const result = taskSchema.safeParse({ ...base, title: 'a'.repeat(201) });
    expect(result.success).toBe(false);
  });

  test('deadline trước startDate bị từ chối', () => {
    const result = taskSchema.safeParse({
      ...base,
      startDate: '2026-03-10T00:00:00.000Z',
      deadline: '2026-03-09T00:00:00.000Z',
    });
    expect(result.success).toBe(false);
  });

  test('deadline bằng startDate hợp lệ', () => {
    const result = taskSchema.safeParse({
      ...base,
      startDate: '2026-03-10T00:00:00.000Z',
      deadline: '2026-03-10T00:00:00.000Z',
    });
    expect(result.success).toBe(true);
  });
});
