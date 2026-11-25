import { auditActorName, formatAuditMetadata } from '../audit';
import { STR } from '../constants';
import type { AuditLogDto } from '../../types/api';

const log: AuditLogDto = {
  id: 'log-1', actorId: 'user-1', actorName: 'Nguyễn An',
  action: 'TASK_CREATED', entityType: 'TASK', entityId: 'task-1',
  createdAt: '2026-11-26T00:00:00Z',
};

test('hiển thị tên, xử lý bản ghi cũ thiếu tên và người thực hiện hệ thống', () => {
  expect(auditActorName(log)).toBe('Nguyễn An');
  expect(auditActorName({ ...log, actorName: undefined })).toBe(STR.auditDetails.unavailableUser);
  expect(auditActorName({ ...log, actorName: ' ', actorId: null })).toBe(STR.auditDetails.system);
});

test('giữ nguyên tên và tiêu đề có dấu bằng', () => {
  expect(formatAuditMetadata('title=Kiểm tra a=b')).toBe('Kiểm tra a=b');
  expect(formatAuditMetadata('name=Dự án A')).toBe('Dự án A');
});

test('dịch trạng thái, vai trò và số người được giao', () => {
  expect(formatAuditMetadata('status=REVIEW')).toBe('Trạng thái · Chờ duyệt');
  expect(formatAuditMetadata('role=VIEWER')).toBe('Vai trò · Chỉ xem');
  expect(formatAuditMetadata('assignees=2')).toBe('Số người được giao · 2');
  expect(STR.audit.ROLE_CHANGED).toBe('Đổi vai trò thành viên');
});

test.each([null, undefined, '', 'title=', 'subtask', 'task=uuid', 'parent=uuid',
  'status=UNKNOWN', 'role=UNKNOWN', 'assignees=uuid'])('bỏ metadata kỹ thuật hoặc thiếu dữ liệu %s', (value) => {
  expect(formatAuditMetadata(value)).toBeNull();
});
