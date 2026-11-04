import dayjs from 'dayjs';
import { avatarColors } from '../theme/tokens';

/** Định dạng ngày tạo dự án (dd/MM/yyyy). */
export function formatDate(iso?: string | null): string {
  if (!iso) return '';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleDateString('vi-VN');
}

/** dd/MM/yyyy HH:mm. */
export function formatDateTime(iso?: string | null): string {
  if (!iso) return '';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  return `${date.toLocaleDateString('vi-VN')} ${date.toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
  })}`;
}

/** HH:mm. */
export function formatTime(iso?: string | null): string {
  if (!iso) return '';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

/** "2 phút trước", "Hôm qua 14:30", "12/03/2026". */
export function formatRelative(iso?: string | null): string {
  if (!iso) return '';
  const date = dayjs(iso);
  if (!date.isValid()) return '';
  const now = dayjs();
  const diffMinutes = now.diff(date, 'minute');

  if (diffMinutes < 1) return 'Vừa xong';
  if (diffMinutes < 60) return `${diffMinutes} phút trước`;
  if (now.isSame(date, 'day')) return formatTime(iso);
  if (now.subtract(1, 'day').isSame(date, 'day')) return `Hôm qua ${formatTime(iso)}`;
  if (now.isSame(date, 'year')) return date.format('DD/MM');
  return date.format('DD/MM/YYYY');
}

/** Hạn hoàn thành đã qua chưa (so với thời điểm hiện tại). */
export function isOverdue(iso?: string | null): boolean {
  if (!iso) return false;
  const date = dayjs(iso);
  return date.isValid() && date.isBefore(dayjs());
}

/** true nếu hai mốc cùng một ngày. */
export function isSameDay(a: string | Date, b: string | Date): boolean {
  return dayjs(a).isSame(dayjs(b), 'day');
}

/** Giá trị cho DateTimePicker (Date). */
export function toDate(iso?: string | null): Date {
  if (!iso) return new Date();
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? new Date() : date;
}

/** ISO string từ Date (giữ nguyên thời điểm). */
export function toIso(value: Date): string {
  return value.toISOString();
}

/** Nhãn ngày kiểu "Hôm nay", "Thứ 5, 12/03". */
export function formatDayLabel(value: string | Date): string {
  const date = dayjs(value);
  if (!date.isValid()) return '';
  const now = dayjs();
  if (now.isSame(date, 'day')) return `Hôm nay, ${date.format('DD/MM')}`;
  if (now.subtract(1, 'day').isSame(date, 'day')) return `Hôm qua, ${date.format('DD/MM')}`;
  if (now.add(1, 'day').isSame(date, 'day')) return `Ngày mai, ${date.format('DD/MM')}`;
  const weekday = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'][date.day()];
  return `${weekday}, ${date.format('DD/MM')}`;
}

/** Tiêu đề tháng "Tháng 3, 2026". */
export function formatMonthTitle(value: string | Date): string {
  return dayjs(value).format('MM/YYYY');
}

/** Dung lượng file dễ đọc. */
export function formatBytes(size: number): string {
  if (!Number.isFinite(size) || size <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const index = Math.min(Math.floor(Math.log(size) / Math.log(1024)), units.length - 1);
  const value = size / 1024 ** index;
  return `${value >= 10 || index === 0 ? Math.round(value) : value.toFixed(1)} ${units[index]}`;
}

/** Lấy chữ cái đầu của họ tên để làm avatar chữ. */
export function initials(fullName?: string | null): string {
  if (!fullName) return '?';
  const parts = fullName.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
}

/** Màu avatar ổn định theo chuỗi (tên/id) — cùng người luôn cùng màu. */
export function colorFromString(value?: string | null): string {
  if (!value) return avatarColors[0];
  let hash = 0;
  for (let index = 0; index < value.length; index += 1) {
    hash = (hash * 31 + value.charCodeAt(index)) % 100000;
  }
  return avatarColors[hash % avatarColors.length];
}

/** Nhãn tiếng Việt cho vai trò dự án. */
export function roleLabel(role: string, labels: Record<string, string>): string {
  return labels[role] ?? role;
}

/** Chuỗi thời gian tương đối cho đầu danh sách chat (Hôm nay → HH:mm, cũ hơn → dd/MM). */
export function formatChatTime(iso?: string | null): string {
  if (!iso) return '';
  const date = dayjs(iso);
  if (!date.isValid()) return '';
  const now = dayjs();
  if (now.isSame(date, 'day')) return date.format('HH:mm');
  if (now.subtract(1, 'day').isSame(date, 'day')) return 'Hôm qua';
  if (now.isSame(date, 'year')) return date.format('DD/MM');
  return date.format('DD/MM/YYYY');
}
