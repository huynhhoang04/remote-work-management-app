import { loginSchema, registerSchema, changePasswordSchema } from '../auth';
import { joinProjectSchema, projectSchema } from '../project';

describe('auth schemas', () => {
  test('login từ chối email sai định dạng', () => {
    const result = loginSchema.safeParse({ email: 'khong-phai-email', password: 'x' });
    expect(result.success).toBe(false);
  });

  test('login hợp lệ', () => {
    const result = loginSchema.safeParse({ email: 'user@example.com', password: 'password-123' });
    expect(result.success).toBe(true);
  });

  test('register từ chối mật khẩu 7 ký tự (backend yêu cầu >= 8)', () => {
    const result = registerSchema.safeParse({
      fullName: 'Nguyen Van A',
      email: 'a@example.com',
      password: '1234567',
      confirmPassword: '1234567',
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((issue) => issue.path.includes('password'))).toBe(true);
    }
  });

  test('register chấp nhận mật khẩu đúng 8 ký tự', () => {
    const result = registerSchema.safeParse({
      fullName: 'Nguyen Van A',
      email: 'a@example.com',
      password: '12345678',
      confirmPassword: '12345678',
    });
    expect(result.success).toBe(true);
  });

  test('register từ chối khi nhập lại mật khẩu không khớp', () => {
    const result = registerSchema.safeParse({
      fullName: 'Nguyen Van A',
      email: 'a@example.com',
      password: 'password-123',
      confirmPassword: 'password-124',
    });
    expect(result.success).toBe(false);
  });

  test('register từ chối mật khẩu > 72 ký tự', () => {
    const longPassword = 'a'.repeat(73);
    const result = registerSchema.safeParse({
      fullName: 'A',
      email: 'a@example.com',
      password: longPassword,
      confirmPassword: longPassword,
    });
    expect(result.success).toBe(false);
  });

  test('change password yêu cầu mật khẩu mới >= 8', () => {
    expect(changePasswordSchema.safeParse({ oldPassword: 'x', newPassword: '1234567' }).success).toBe(false);
    expect(changePasswordSchema.safeParse({ oldPassword: 'x', newPassword: '12345678' }).success).toBe(true);
  });
});

describe('project schemas', () => {
  test('tên dự án rỗng bị chặn', () => {
    expect(projectSchema.safeParse({ name: '' }).success).toBe(false);
  });

  test('tên dự án > 100 ký tự bị chặn', () => {
    expect(projectSchema.safeParse({ name: 'a'.repeat(101) }).success).toBe(false);
  });

  test('mã mời phải đúng 12 ký tự [A-Z0-9]', () => {
    expect(joinProjectSchema.safeParse({ code: 'ABC123' }).success).toBe(false);
    expect(joinProjectSchema.safeParse({ code: 'abc123def456' }).success).toBe(false);
    expect(joinProjectSchema.safeParse({ code: 'ABC123DEF456' }).success).toBe(true);
  });
});
