import React, { useEffect, useRef } from 'react';
import { Animated, StyleSheet } from 'react-native';
import { Text, useTheme } from 'react-native-paper';
import { useServerSlow } from '../api/activity';
import { spacing } from '../theme/tokens';
import { STR } from '../utils/constants';

/**
 * Banner "Máy chủ đang khởi động" khi request chạy > 3s (Render free cold start).
 * @reference docs/plan-app/01-phase-1.md §6
 */
export function ServerWakeBanner() {
  const slow = useServerSlow();
  const theme = useTheme();
  const opacity = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.timing(opacity, { toValue: slow ? 1 : 0, duration: 250, useNativeDriver: true }).start();
  }, [slow, opacity]);

  if (!slow) return null;

  return (
    <Animated.View
      pointerEvents="none"
      style={[styles.banner, { backgroundColor: theme.colors.secondaryContainer, opacity }]}
    >
      <Text variant="labelMedium" style={{ color: theme.colors.onSecondaryContainer }}>
        {STR.serverWakingUp}
      </Text>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  banner: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    zIndex: 100,
    paddingVertical: spacing.xs,
    paddingHorizontal: spacing.lg,
    alignItems: 'center',
  },
});
