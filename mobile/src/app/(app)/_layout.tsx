import { Redirect, Stack } from 'expo-router';
import React from 'react';
import { useTheme } from 'react-native-paper';
import { useSession } from '@/store/session';
import { STR } from '@/utils/constants';

/**
 * Layout nhóm màn hình sau đăng nhập — chặn khi chưa có phiên.
 * Tabs là màn chính; chi tiết (dự án, task, lịch, chat) push đè lên.
 * Mỗi màn chi tiết tự khai báo tiêu đề qua <Stack.Screen options>.
 * @reference docs/plan-app/02-phase-2.md §2
 */
export default function AppLayout() {
  const { isReady, isAuthenticated } = useSession();
  const theme = useTheme();

  if (!isReady) return null;
  if (!isAuthenticated) return <Redirect href="/login" />;

  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: theme.colors.surface },
        headerTintColor: theme.colors.onSurface,
        headerTitleStyle: { fontWeight: '600', fontSize: 18 },
        headerShadowVisible: false,
        headerBackButtonDisplayMode: 'minimal',
        contentStyle: { backgroundColor: theme.colors.background },
      }}
    >
      <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
      <Stack.Screen name="projects/new" options={{ title: STR.projects.createTitle }} />
      <Stack.Screen name="projects/join" options={{ title: STR.projects.joinTitle }} />
    </Stack>
  );
}
