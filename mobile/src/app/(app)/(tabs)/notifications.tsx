import { router } from 'expo-router';
import React, { useCallback, useMemo, useState } from 'react';
import { ActivityIndicator, FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { useSnackbar } from '@/components/SnackbarProvider';
import { NotificationRow } from '@/components/NotificationRow';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { TabHeader } from '@/components/TabHeader';
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
  useUnreadNotificationCount,
} from '@/features/notifications/hooks';
import { spacing } from '@/theme/tokens';
import type { NotificationDto } from '@/types/api';
import { STR } from '@/utils/constants';

/**
 * Tab Thông báo: phân trang, đọc 1/tất cả, deep-link tới task/chat.
 * @reference docs/plan-app/02-phase-2.md §5.7
 */
export default function NotificationsScreen() {
  const theme = useTheme();
  const { show } = useSnackbar();
  const notifications = useNotifications();
  const unread = useUnreadNotificationCount();
  const markRead = useMarkNotificationRead();
  const markAllRead = useMarkAllNotificationsRead();
  const [refreshing, setRefreshing] = useState(false);

  const items = useMemo(
    () => notifications.data?.pages.flatMap((page) => page.content) ?? [],
    [notifications.data],
  );
  const unreadCount = unread.data?.count ?? 0;

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await Promise.all([notifications.refetch(), unread.refetch()]);
    } finally {
      setRefreshing(false);
    }
  }, [notifications, unread]);

  const onMarkAll = useCallback(async () => {
    try {
      await markAllRead.mutateAsync();
      show(STR.notifications.allRead);
    } catch (error) {
      show(normalizeError(error).message);
    }
  }, [markAllRead, show]);

  const onOpen = useCallback(
    (notification: NotificationDto) => {
      if (!notification.readAt) {
        markRead.mutate(notification.id);
      }
      if (!notification.refId) return;
      if (notification.refType === 'TASK') {
        router.push(`/tasks/${notification.refId}`);
      } else if (notification.refType === 'CONVERSATION') {
        router.push(`/chat/${notification.refId}`);
      }
    },
    [markRead],
  );

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <TabHeader
        title={STR.notifications.title}
        subtitle={unreadCount > 0 ? `${unreadCount} ${STR.notifications.unread}` : undefined}
        actions={[
          {
            icon: 'email-open-outline',
            onPress: onMarkAll,
            badge: unreadCount,
            accessibilityLabel: STR.notifications.markAllRead,
          },
        ]}
      />

      {notifications.isPending ? (
        <View style={styles.skeleton}>
          <SkeletonList count={5} />
        </View>
      ) : notifications.isError ? (
        <StateView
          status="error"
          errorText={normalizeError(notifications.error).message}
          onRetry={() => notifications.refetch()}
        />
      ) : items.length === 0 ? (
        <StateView
          status="empty"
          icon="bell-outline"
          emptyText={STR.notifications.empty}
          emptyHint={STR.notifications.emptyHint}
        />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => <NotificationRow notification={item} onPress={onOpen} />}
          contentContainerStyle={styles.list}
          onEndReachedThreshold={0.4}
          onEndReached={() => {
            if (notifications.hasNextPage && !notifications.isFetchingNextPage) {
              void notifications.fetchNextPage();
            }
          }}
          ListFooterComponent={
            notifications.isFetchingNextPage ? <ActivityIndicator style={styles.footer} /> : undefined
          }
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={theme.colors.primary} />
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  skeleton: { paddingTop: spacing.sm },
  list: { paddingBottom: spacing.xxl },
  footer: { paddingVertical: spacing.lg },
});
