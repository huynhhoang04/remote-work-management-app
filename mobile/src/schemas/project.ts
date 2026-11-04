import { z } from 'zod';

/** Schema tạo/sửa dự án — khớp ProjectRequest backend (name 1–100, description ≤ 1000). */
export const projectSchema = z.object({
  name: z
    .string()
    .min(1, 'Tên dự án không được để trống')
    .max(100, 'Tên dự án tối đa 100 ký tự'),
  description: z.string().max(1000, 'Mô tả tối đa 1000 ký tự').optional(),
});

/** Schema tham gia dự án — mã mời 12 ký tự [A-Z0-9]. */
export const joinProjectSchema = z.object({
  code: z
    .string()
    .min(1, 'Mã mời không được để trống')
    .regex(/^[A-Z0-9]{12}$/, 'Mã mời gồm 12 ký tự chữ và số'),
});

/** Nhãn dự án — name ≤ 50, color ≤ 20. */
export const labelSchema = z.object({
  name: z
    .string()
    .min(1, 'Tên nhãn không được để trống')
    .max(50, 'Tên nhãn tối đa 50 ký tự'),
  color: z
    .string()
    .min(1, 'Màu nhãn không được để trống')
    .max(20, 'Màu nhãn tối đa 20 ký tự'),
});

export type ProjectForm = z.infer<typeof projectSchema>;
export type JoinProjectForm = z.infer<typeof joinProjectSchema>;
export type LabelForm = z.infer<typeof labelSchema>;
