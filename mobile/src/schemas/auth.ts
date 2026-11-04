import { z } from 'zod';

/**
 * Schema validate form — phải khớp luật backend
 * (RegisterRequest @Size(min=8,max=72), @Email...).
 * @reference docs/plan-app/01-phase-1.md §7
 */
export const loginSchema = z.object({
  email: z
    .string()
    .min(1, 'Email không được để trống')
    .email('Email không hợp lệ'),
  password: z.string().min(1, 'Mật khẩu không được để trống'),
});

export const registerSchema = z
  .object({
    fullName: z
      .string()
      .min(1, 'Họ tên không được để trống')
      .max(100, 'Họ tên tối đa 100 ký tự'),
    email: z
      .string()
      .min(1, 'Email không được để trống')
      .email('Email không hợp lệ'),
    password: z
      .string()
      .min(8, 'Mật khẩu phải từ 8 đến 72 ký tự')
      .max(72, 'Mật khẩu phải từ 8 đến 72 ký tự'),
    confirmPassword: z.string().min(1, 'Nhập lại mật khẩu'),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Mật khẩu nhập lại không khớp',
    path: ['confirmPassword'],
  });

export const forgotPasswordSchema = z.object({
  email: z
    .string()
    .min(1, 'Email không được để trống')
    .email('Email không hợp lệ'),
});

export const changePasswordSchema = z.object({
  oldPassword: z.string().min(1, 'Mật khẩu hiện tại không được để trống'),
  newPassword: z
    .string()
    .min(8, 'Mật khẩu phải từ 8 đến 72 ký tự')
    .max(72, 'Mật khẩu phải từ 8 đến 72 ký tự'),
});

export type LoginForm = z.infer<typeof loginSchema>;
export type RegisterForm = z.infer<typeof registerSchema>;
export type ForgotPasswordForm = z.infer<typeof forgotPasswordSchema>;
export type ChangePasswordForm = z.infer<typeof changePasswordSchema>;
