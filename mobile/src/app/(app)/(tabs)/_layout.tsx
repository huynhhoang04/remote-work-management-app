import { Tabs } from 'expo-router';
import React from 'react';
import { Icon, useTheme } from 'react-native-paper';
import { useConversations } from '@/features/chat/hooks';
import { useUnreadNotificationCount } from '@/features/notifications/hooks';
import { STR } from '@/utils/constants';

/**
 * Bottom tabs: Hôm nay · Dự án · Chat · Thông báo · Hồ sơ.
 * @reference docs/plan-app/02-phase-2.md §2
 */
export default function TabsLayout() {
  const theme = useTheme();
  const unreadNotifications = useUnreadNotificationCount();
  const conversations = useConversations();

  const notificationCount = unreadNotifications.data?.count ?? 0;
  const chatCount =
    conversations.data?.reduce((total, conversation) => total + conversation.unreadCount, 0) ?? 0;

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: theme.colors.primary,
        tabBarInactiveTintColor: theme.colors.onSurfaceVariant,
        tabBarStyle: {
          backgroundColor: theme.colors.surface,
          borderTopColor: theme.colors.outline,
        },
        tabBarLabelStyle: { fontSize: 11, fontWeight: '600' },
        tabBarBadgeStyle: { backgroundColor: theme.colors.error, fontSize: 10 },
      }}
    >
      <Tabs.Screen
        name="today"
        options={{
          title: STR.today.title,
          tabBarIcon: ({ color, size }) => <Icon source="home-variant-outline" color={color as string} size={size} />,
        }}
      />
      <Tabs.Screen
        name="projects"
        options={{
          title: STR.projects.title,
          tabBarIcon: ({ color, size }) => <Icon source="view-dashboard-outline" color={color as string} size={size} />,
        }}
      />
      <Tabs.Screen
        name="chat"
        options={{
          title: STR.chat.title,
          tabBarBadge: chatCount > 0 ? (chatCount > 99 ? '99+' : chatCount) : undefined,
          tabBarIcon: ({ color, size }) => <Icon source="message-text-outline" color={color as string} size={size} />,
        }}
      />
      <Tabs.Screen
        name="notifications"
        options={{
          title: STR.notifications.title,
          tabBarBadge:
            notificationCount > 0 ? (notificationCount > 99 ? '99+' : notificationCount) : undefined,
          tabBarIcon: ({ color, size }) => <Icon source="bell-outline" color={color as string} size={size} />,
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: STR.profile.title,
          tabBarIcon: ({ color, size }) => <Icon source="account-circle-outline" color={color as string} size={size} />,
        }}
      />
    </Tabs>
  );
}
