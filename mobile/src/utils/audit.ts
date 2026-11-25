import type { AuditLogDto } from '../types/api';
import { STR } from './constants';

/** Tên người thực hiện, hỗ trợ bản ghi hệ thống và API cũ thiếu actorName. */
export function auditActorName(log: AuditLogDto): string {
  return log.actorName?.trim()
    || (log.actorId ? STR.auditDetails.unavailableUser : STR.auditDetails.system);
}

/** Diễn giải metadata cũ dạng key=value, giữ nguyên nội dung tên/tiêu đề. */
export function formatAuditMetadata(metadata?: string | null): string | null {
  if (!metadata) return null;
  const separator = metadata.indexOf('=');
  const key = metadata.slice(0, separator);
  const value = metadata.slice(separator + 1).trim();
  if (separator < 0 || !value) return null;

  switch (key) {
    case 'title':
    case 'name':
      return value;
    case 'status': {
      const status = STR.tasks.statuses[value];
      return status ? `${STR.auditDetails.status} · ${status}` : null;
    }
    case 'role': {
      const role = (STR.roles as Record<string, string>)[value];
      return role ? `${STR.auditDetails.role} · ${role}` : null;
    }
    case 'assignees':
      return /^\d+$/.test(value) ? `${STR.auditDetails.assignees} · ${value}` : null;
    default:
      return null;
  }
}
