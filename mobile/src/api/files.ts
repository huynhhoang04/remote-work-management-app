import { Directory, File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import type { ImageSourcePropType } from 'react-native';
import type { FileDto, UserDto } from '../types/api';
import { api, API_BASE_URL } from './client';
import { getAccessToken } from './tokenStore';

/** Tệp người dùng chọn từ thiết bị (document picker / image picker). */
export interface UploadableFile {
  uri: string;
  name: string;
  mimeType?: string | null;
  size?: number | null;
}

/** Ghép URL tương đối backend (`/api/v1/files/{id}`) thành URL tuyệt đối. */
export function absoluteUrl(path?: string | null): string | null {
  if (!path) return null;
  if (/^https?:\/\//i.test(path)) return path;
  return `${API_BASE_URL}${path.startsWith('/') ? '' : '/'}${path}`;
}

/**
 * Nguồn ảnh cần Bearer token (avatar/file đính kèm) — RN Image hỗ trợ headers.
 * @reference docs/plan-app/02-phase-2.md §3
 */
export function authImageSource(path?: string | null): ImageSourcePropType | undefined {
  const uri = absoluteUrl(path);
  if (!uri) return undefined;
  const token = getAccessToken();
  return token ? { uri, headers: { Authorization: `Bearer ${token}` } } : { uri };
}

function toFormData(file: UploadableFile): FormData {
  const form = new FormData();
  form.append('file', {
    uri: file.uri,
    name: file.name,
    type: file.mimeType ?? 'application/octet-stream',
  } as unknown as Blob);
  return form;
}

/** Upload multipart dùng chung — backend nhận `@RequestPart("file")`. */
export async function uploadMultipart(
  path: string,
  file: UploadableFile,
  query?: Record<string, string>,
): Promise<FileDto> {
  const { data } = await api.post<FileDto>(path, toFormData(file), {
    headers: { 'Content-Type': 'multipart/form-data' },
    params: query,
  });
  return data;
}

/** API file — docs/plan-backend/02-danh-sach-api.md §11. */
export const filesApi = {
  upload(file: UploadableFile, projectId?: string): Promise<FileDto> {
    return uploadMultipart('/files', file, projectId ? { projectId } : undefined);
  },

  async uploadAvatar(file: UploadableFile): Promise<UserDto> {
    const { data } = await api.post<UserDto>('/users/me/avatar', toFormData(file), {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
  },

  /**
   * Tải file (có Bearer) vào cache rồi mở hộp chia sẻ hệ thống.
   * @reference docs/plan-app/02-phase-2.md §3
   */
  async downloadAndShare(file: FileDto): Promise<void> {
    const url = absoluteUrl(file.url);
    if (!url) throw new Error('File URL không hợp lệ');
    const token = getAccessToken();
    const safeName = file.name.replace(/[^\w.\-() ]+/g, '_') || `file-${file.id}`;
    const directory = new Directory(Paths.cache, 'downloads');
    if (!directory.exists) directory.create({ intermediates: true, idempotent: true });
    const destination = new File(directory, `${file.id}-${safeName}`);
    const downloaded = await File.downloadFileAsync(url, destination, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      idempotent: true,
    });
    if (await Sharing.isAvailableAsync()) {
      await Sharing.shareAsync(downloaded.uri, { mimeType: file.mimeType, dialogTitle: file.name });
    }
  },
};
