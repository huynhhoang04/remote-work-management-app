import '@/utils/polyfills';

import { QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import React, { useEffect } from 'react';
import { useColorScheme } from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ServerWakeBanner } from '@/components/ServerWakeBanner';
import { SnackbarProvider } from '@/components/SnackbarProvider';
import { RealtimeBridge } from '@/features/notifications/RealtimeBridge';
import { configureForegroundNotifications } from '@/features/push/push';
import { queryClient } from '@/store/queryClient';
import { SessionProvider } from '@/store/session';
import { getPaperTheme } from '@/theme/paperTheme';

/**
 * Root layout: providers + Stack toàn app.
 * @reference docs/plan-app/01-phase-1.md §5.10
 */
export default function RootLayout() {
  const scheme = useColorScheme() === 'dark' ? 'dark' : 'light';
  const theme = getPaperTheme(scheme);

  useEffect(() => {
    configureForegroundNotifications();
  }, []);

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <PaperProvider theme={theme}>
          <QueryClientProvider client={queryClient}>
            <SessionProvider>
              <SnackbarProvider>
                <ServerWakeBanner />
                <RealtimeBridge />
                <Stack screenOptions={{ headerShown: false }}>
                  <Stack.Screen name="index" />
                  <Stack.Screen name="(auth)" />
                  <Stack.Screen name="(app)" />
                </Stack>
                <StatusBar style={scheme === 'dark' ? 'light' : 'dark'} />
              </SnackbarProvider>
            </SessionProvider>
          </QueryClientProvider>
        </PaperProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
