import { scheduleSchema } from '../schedule';

const base = {
  title: 'Họp nhóm',
  startTime: '2026-03-10T09:00:00.000Z',
  endTime: '2026-03-10T10:00:00.000Z',
};

describe('scheduleSchema', () => {
  test('hợp lệ khi endTime sau startTime', () => {
    expect(scheduleSchema.safeParse(base).success).toBe(true);
  });

  test('endTime trước startTime bị từ chối', () => {
    const result = scheduleSchema.safeParse({
      ...base,
      endTime: '2026-03-10T08:00:00.000Z',
    });
    expect(result.success).toBe(false);
  });

  test('endTime bằng startTime bị từ chối', () => {
    expect(
      scheduleSchema.safeParse({ ...base, endTime: base.startTime }).success,
    ).toBe(false);
  });

  test('thiếu tiêu đề bị từ chối', () => {
    expect(scheduleSchema.safeParse({ ...base, title: '' }).success).toBe(false);
  });
});
