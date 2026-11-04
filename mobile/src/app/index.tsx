import { Redirect } from 'expo-router';
import React from 'react';
import { Image, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Text, useTheme } from 'react-native-paper';
import logo from '@/assets/images/logo.png';
import { useSession } from '@/store/session';
import { spacing } from '@/theme/tokens';
import { STR } from '@/utils/constants';

/**
 * Splash: chờ đọc phiên từ SecureStore rồi điều hướng.
 * @reference docs/plan-app/01-phase-1.md §5.1
 */
export default function IndexScreen() {
  const { isReady, isAuthenticated } = useSession();
  const theme = useTheme();

  if (!isReady) {
    return (
      <View style={[styles.center, { backgroundColor: theme.colors.background }]}>
        <Image source={logo} style={styles.logo} resizeMode="contain" />
        <Text variant="headlineSmall" style={styles.appName}>
          {STR.appName}
        </Text>
        <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant }}>
          {STR.splash}
        </Text>
        <ActivityIndicator size="small" style={styles.spinner} />
      </View>
    );
  }

  return <Redirect href={isAuthenticated ? '/today' : '/login'} />;
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: spacing.sm },
  logo: { width: 120, height: 120, borderRadius: 28 },
  appName: { fontWeight: '700', marginTop: spacing.sm },
  spinner: { marginTop: spacing.lg },
});
