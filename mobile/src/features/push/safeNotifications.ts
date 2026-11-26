import Constants, { ExecutionEnvironment } from 'expo-constants';
import type * as NotificationsType from 'expo-notifications';

const isExpoGo =
  Constants.appOwnership === 'expo' ||
  Constants.executionEnvironment === ExecutionEnvironment.StoreClient;

/**
 * An toàn cho Expo Go: `expo-notifications` yêu cầu native module
 * `ExpoTopicSubscriptionModule` vốn không có sẵn trên Expo Go (Android SDK 53+).
 * Bỏ qua `require('expo-notifications')` khi đang chạy trong Expo Go để không gây crash hay log lỗi.
 */
function loadNotificationsModule(): typeof NotificationsType | null {
  if (isExpoGo) {
    return null;
  }
  try {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    return require('expo-notifications') as typeof NotificationsType;
  } catch {
    return null;
  }
}

export const Notifications = loadNotificationsModule();
