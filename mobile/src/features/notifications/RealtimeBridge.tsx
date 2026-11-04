import { useQueryClient } from '@tanstack/react-query';
import * as Notifications from 'expo-notifications';
import { router } from 'expo-router';
import { useEffect } from 'react';
import { parseStompBody, realtime } from '../../api/ws';
import { useSession } from '../../store/session';
import type { NotificationDto, UnreadCountDto } from '../../types/api';
import { registerForPushNotifications, unregisterPushNotifications } from '../push/push';

interface PushData {
  refType?: string;
  refId?: string;
}

function openFromNotification(data: PushData | undefined): void {
  if (!data?.refId) return;
  if (data.refType === 'TASK') {
    router.push(`/tasks/${data.refId}`);
  } else if (data.refType === 'CONVERSATION') {
    router.push(`/chat/${data.refId}`);
  }
}

function dataOf(notification: Notifications.Notification): PushData | undefined {
  return notification.request.content.data as PushData | undefined;
}

/**
 * Cầu nối realtime toàn app: STOMP notification, badge chưa đọc, đăng ký push,
 * điều hướng khi bấm thông báo.
 * @reference docs/plan-app/02-phase-2.md §5.7, §5.10
 */
export function RealtimeBridge() {
  const { isAuthenticated } = useSession();
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!isAuthenticated) return;

    realtime.start();
    void registerForPushNotifications();

    const unsubscribe = realtime.subscribe('/user/queue/notifications', (message) => {
      const notification = parseStompBody<NotificationDto>(message);
      if (!notification) return;
      queryClient.setQueryData<UnreadCountDto>(['notifications', 'unread'], (current) => ({
        count: (current?.count ?? 0) + 1,
      }));
      void queryClient.invalidateQueries({ queryKey: ['notifications'], exact: true });
      void queryClient.invalidateQueries({ queryKey: ['conversations'] });
    });

    const responseListener = Notifications.addNotificationResponseReceivedListener((response) => {
      openFromNotification(dataOf(response.notification));
    });

    void Notifications.getLastNotificationResponseAsync().then((response) => {
      if (response) openFromNotification(dataOf(response.notification));
    });

    return () => {
      unsubscribe();
      responseListener.remove();
      void realtime.stop();
      void unregisterPushNotifications();
    };
  }, [isAuthenticated, queryClient]);

  return null;
}
