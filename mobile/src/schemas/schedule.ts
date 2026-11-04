import { z } from 'zod';

/** Sự kiện lịch — title ≤ 200, description ≤ 2000, endTime > startTime. */
export const scheduleSchema = z
  .object({
    title: z
      .string()
      .min(1, 'Tiêu đề không được để trống')
      .max(200, 'Tiêu đề tối đa 200 ký tự'),
    description: z.string().max(2000, 'Mô tả tối đa 2000 ký tự').optional(),
    startTime: z.string().min(1, 'Chọn thời gian bắt đầu'),
    endTime: z.string().min(1, 'Chọn thời gian kết thúc'),
    taskId: z.string().nullable().optional(),
  })
  .refine((values) => new Date(values.endTime).getTime() > new Date(values.startTime).getTime(), {
    message: 'Thời gian kết thúc phải sau thời gian bắt đầu',
    path: ['endTime'],
  });

export type ScheduleForm = z.infer<typeof scheduleSchema>;

/** Validate phần text của form sự kiện (thời gian kiểm tra riêng trong form). */
export const scheduleTextSchema = z.object({
  title: z
    .string()
    .min(1, 'Tiêu đề không được để trống')
    .max(200, 'Tiêu đề tối đa 200 ký tự'),
  description: z.string().max(2000, 'Mô tả tối đa 2000 ký tự').optional(),
});

export type ScheduleTextForm = z.infer<typeof scheduleTextSchema>;
