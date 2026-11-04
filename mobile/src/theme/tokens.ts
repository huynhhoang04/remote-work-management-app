/**
 * Design tokens dùng chung toàn app — không hardcode màu/spacing ở màn hình.
 * Palette lấy theo logo (navy #101F34 + xanh #2C63E8 + cam #FD843D).
 * @reference docs/plan-app/01-phase-1.md §4.1
 */
export type AppColorScheme = 'light' | 'dark';

export const colors = {
  light: {
    primary: '#2C63E8',
    onPrimary: '#FFFFFF',
    primaryContainer: '#E7EEFF',
    onPrimaryContainer: '#132C6B',
    accent: '#FD843D',
    surface: '#FFFFFF',
    surfaceVariant: '#F4F6FB',
    onSurface: '#111827',
    onSurfaceVariant: '#5C6675',
    outline: '#D4DAE5',
    error: '#D32F2F',
    onError: '#FFFFFF',
    success: '#2E7D32',
    navy: '#101F34',
  },
  dark: {
    primary: '#A9C0FF',
    onPrimary: '#0B1B44',
    primaryContainer: '#24408F',
    onPrimaryContainer: '#DCE6FF',
    accent: '#FD843D',
    surface: '#101722',
    surfaceVariant: '#1A2230',
    onSurface: '#E6EAF1',
    onSurfaceVariant: '#A8B3C2',
    outline: '#3A4657',
    error: '#FF8A80',
    onError: '#4A0E0E',
    success: '#7BC47F',
    navy: '#0B1422',
  },
} as const;

/** Màu chip vai trò trong dự án. */
export const roleColors: Record<string, string> = {
  OWNER: '#F97316',
  ADMIN: '#7C3AED',
  MEMBER: '#2C63E8',
  VIEWER: '#64748B',
};

/** Màu trạng thái công việc (Kanban + chip). */
export const statusColors: Record<string, string> = {
  TODO: '#64748B',
  DOING: '#2C63E8',
  REVIEW: '#F59E0B',
  DONE: '#16A34A',
};

/** Màu độ ưu tiên. */
export const priorityColors: Record<string, string> = {
  LOW: '#64748B',
  MEDIUM: '#0EA5E9',
  HIGH: '#F97316',
  URGENT: '#DC2626',
};

/** Màu avatar chữ cái (chọn theo hash tên — ổn định giữa các lần render). */
export const avatarColors = ['#2C63E8', '#7C3AED', '#0EA5E9', '#F97316', '#16A34A', '#DB2777'];

export const spacing = { xs: 4, sm: 8, md: 12, lg: 16, xl: 24, xxl: 32 } as const;

export const radius = { xs: 4, sm: 8, md: 12, lg: 16, xl: 20, pill: 999 } as const;

export const typography = {
  title: { fontSize: 20, fontWeight: '700' as const, letterSpacing: 0.1 },
  subtitle: { fontSize: 16, fontWeight: '600' as const, letterSpacing: 0.1 },
  body: { fontSize: 14, fontWeight: '400' as const, letterSpacing: 0.2 },
  caption: { fontSize: 12, fontWeight: '400' as const, letterSpacing: 0.3 },
  label: { fontSize: 11, fontWeight: '600' as const, letterSpacing: 0.4 },
};
