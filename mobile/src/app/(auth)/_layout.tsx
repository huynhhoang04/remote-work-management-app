import { Redirect, Stack } from 'expo-router';
import React from 'react';
import { useSession } from '@/store/session';

/**
 * Layout nhóm màn hình chưa đăng nhập — đã đăng nhập thì chuyển vào app.
 * @reference docs/plan-app/01-phase-1.md §5.10
 */
export default function AuthLayout() {
  const { isReady, isAuthenticated } = useSession();

  if (isReady && isAuthenticated) {
    return <Redirect href="/today" />;
  }

  return <Stack screenOptions={{ headerShown: false }} />;
}
