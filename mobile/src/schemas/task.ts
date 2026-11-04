import { z } from 'zod';

const instant = z.string().min(1);

/** Schema tạo/sửa task — khớp TaskRequest/TaskUpdateRequest backend. */
export const taskSchema = z
  .object({
    title: z
      .string()
      .min(1, 'Tiêu đề không được để trống')
      .max(200, 'Tiêu đề tối đa 200 ký tự'),
    description: z.string().max(5000, 'Mô tả tối đa 5000 ký tự').optional(),
    status: z.enum(['TODO', 'DOING', 'REVIEW', 'DONE']),
    priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']),
    startDate: instant.nullable().optional(),
    deadline: instant.nullable().optional(),
  })
  .refine(
    (values) => {
      if (!values.startDate || !values.deadline) return true;
      return new Date(values.deadline).getTime() >= new Date(values.startDate).getTime();
    },
    { message: 'Hạn hoàn thành phải sau ngày bắt đầu', path: ['deadline'] },
  );

export const commentSchema = z.object({
  content: z
    .string()
    .min(1, 'Nội dung bình luận không được để trống')
    .max(2000, 'Bình luận tối đa 2000 ký tự'),
});

export const subtaskSchema = z.object({
  title: z
    .string()
    .min(1, 'Nội dung việc con không được để trống')
    .max(200, 'Việc con tối đa 200 ký tự'),
});

export type TaskForm = z.infer<typeof taskSchema>;
export type CommentForm = z.infer<typeof commentSchema>;
export type SubtaskForm = z.infer<typeof subtaskSchema>;

/** Validate phần text của form task (schema đầy đủ dùng khi submit). */
export const taskTextSchema = z.object({
  title: z
    .string()
    .min(1, 'Tiêu đề không được để trống')
    .max(200, 'Tiêu đề tối đa 200 ký tự'),
  description: z.string().max(5000, 'Mô tả tối đa 5000 ký tự').optional(),
});

export type TaskTextForm = z.infer<typeof taskTextSchema>;
