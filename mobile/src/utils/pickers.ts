import * as DocumentPicker from 'expo-document-picker';
import * as ImagePicker from 'expo-image-picker';
import type { UploadableFile } from '../api/files';

/** Chọn tệp bất kỳ từ thiết bị. */
export async function pickDocument(): Promise<UploadableFile | null> {
  const result = await DocumentPicker.getDocumentAsync({
    copyToCacheDirectory: true,
    multiple: false,
  });
  if (result.canceled || !result.assets[0]) return null;
  const asset = result.assets[0];
  return {
    uri: asset.uri,
    name: asset.name ?? `file-${Date.now()}`,
    mimeType: asset.mimeType ?? 'application/octet-stream',
    size: asset.size,
  };
}

/** Chọn ảnh từ thư viện hoặc chụp ảnh. */
export async function pickImage(fromCamera = false): Promise<UploadableFile | null> {
  if (fromCamera) {
    const permission = await ImagePicker.requestCameraPermissionsAsync();
    if (!permission.granted) return null;
    const result = await ImagePicker.launchCameraAsync({ quality: 0.7 });
    if (result.canceled || !result.assets[0]) return null;
    const asset = result.assets[0];
    return {
      uri: asset.uri,
      name: asset.fileName ?? `photo-${Date.now()}.jpg`,
      mimeType: asset.mimeType ?? 'image/jpeg',
      size: asset.fileSize,
    };
  }

  const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
  if (!permission.granted) return null;
  const result = await ImagePicker.launchImageLibraryAsync({ quality: 0.7, mediaTypes: ['images'] });
  if (result.canceled || !result.assets[0]) return null;
  const asset = result.assets[0];
  return {
    uri: asset.uri,
    name: asset.fileName ?? `image-${Date.now()}.jpg`,
    mimeType: asset.mimeType ?? 'image/jpeg',
    size: asset.fileSize,
  };
}
