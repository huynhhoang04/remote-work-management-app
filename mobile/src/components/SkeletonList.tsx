import React, { useEffect, useRef } from 'react';
import { Animated, StyleSheet, View } from 'react-native';
import { useTheme } from 'react-native-paper';
import { radius, spacing } from '../theme/tokens';

interface Props {
  count?: number;
}

/**
 * Skeleton loading (pulse) thay cho spinner khi tải danh sách.
 * @reference docs/plan-app/01-phase-1.md §4.2
 */
export function SkeletonList({ count = 4 }: Props) {
  const theme = useTheme();
  const opacity = useRef(new Animated.Value(0.45)).current;

  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(opacity, { toValue: 1, duration: 700, useNativeDriver: true }),
        Animated.timing(opacity, { toValue: 0.45, duration: 700, useNativeDriver: true }),
      ]),
    );
    loop.start();
    return () => loop.stop();
  }, [opacity]);

  return (
    <View>
      {Array.from({ length: count }).map((_, index) => (
        <Animated.View
          key={index}
          style={[styles.item, { backgroundColor: theme.colors.surfaceVariant, opacity }]}
        >
          <View style={[styles.circle, { backgroundColor: theme.colors.outline }]} />
          <View style={styles.lines}>
            <View style={[styles.line, styles.title, { backgroundColor: theme.colors.outline }]} />
            <View style={[styles.line, styles.body, { backgroundColor: theme.colors.outline }]} />
          </View>
        </Animated.View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  item: {
    flexDirection: 'row',
    alignItems: 'center',
    marginHorizontal: spacing.lg,
    marginBottom: spacing.md,
    padding: spacing.lg,
    borderRadius: radius.lg,
  },
  circle: { width: 44, height: 44, borderRadius: 22, opacity: 0.25 },
  lines: { flex: 1, marginLeft: spacing.md },
  line: { borderRadius: radius.sm, opacity: 0.25 },
  title: { height: 16, width: '60%' },
  body: { height: 12, width: '85%', marginTop: spacing.sm },
});
