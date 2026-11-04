import * as Device from 'expo-device';
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { devicesApi } from '../../api/devices';

let registeredToken: string | null = null;

/** Cấu hình cách hiển thị thông báo khi app đang mở. */
export function configureForegroundNotifications(): void {
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowBanner: true,
      shouldShowList: true,
      shouldPlaySound: false,
      shouldSetBadge: false,
    }),
  });
}

/**
 * Xin quyền + đăng ký FCM device token với backend.
 * Trả null khi thiếu quyền/cấu hình (Expo Go, emulator, chưa có google-services.json).
 * @reference docs/plan-app/02-phase-2.md §5.10
 */
export async function registerForPushNotifications(): Promise<string | null> {
  if (Platform.OS === 'web' || !Device.isDevice) return null;

  try {
    const existing = await Notifications.getPermissionsAsync();
    let status = existing.status;
    if (status !== 'granted') {
      status = (await Notifications.requestPermissionsAsync()).status;
    }
    if (status !== 'granted') return null;

    if (Platform.OS === 'android') {
      await Notifications.setNotificationChannelAsync('default', {
        name: 'Thông báo',
        importance: Notifications.AndroidImportance.HIGH,
        vibrationPattern: [0, 250, 250, 250],
        lightColor: '#2C63E8',
      });
    }

    const deviceToken = await Notifications.getDevicePushTokenAsync();
    const token = deviceToken.data;
    if (!token) return null;

    registeredToken = token;
    await devicesApi.register(token, Platform.OS.toUpperCase());
    return token;
  } catch {
    // Thiếu Firebase config / chạy Expo Go → bỏ qua, app vẫn dùng trung tâm thông báo.
    return null;
  }
}

/** Huỷ device token khi đăng xuất (best effort). */
export async function unregisterPushNotifications(): Promise<void> {
  if (!registeredToken) return;
  const token = registeredToken;
  registeredToken = null;
  try {
    await devicesApi.unregister(token);
  } catch {
    // Token có thể đã bị thu hồi — bỏ qua.
  }
}
