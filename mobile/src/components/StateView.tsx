import React from 'react';
import { StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Icon, Text, useTheme } from 'react-native-paper';
import { radius, spacing } from '../theme/tokens';
import { STR } from '../utils/constants';

export type StateStatus = 'loading' | 'empty' | 'error' | 'content';

interface Props {
  status: StateStatus;
  onRetry?: () => void;
  emptyText?: string;
  emptyHint?: string;
  errorText?: string;
  icon?: string;
  children?: React.ReactNode;
}

/**
 * Trạng thái dùng chung: loading / empty / error+retry / content.
 * @reference docs/plan-app/01-phase-1.md §4.3
 */
export function StateView({ status, onRetry, emptyText, emptyHint, errorText, icon, children }: Props) {
  const theme = useTheme();

  if (status === 'loading') {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (status === 'empty') {
    return (
      <View style={styles.center}>
        <View style={[styles.iconBadge, { backgroundColor: theme.colors.primaryContainer }]}>
          <Icon source={icon ?? 'folder-plus-outline'} size={40} color={theme.colors.primary} />
        </View>
        <Text variant="titleMedium" style={styles.title}>
          {emptyText ?? STR.projects.empty}
        </Text>
        {emptyHint ? (
          <Text variant="bodyMedium" style={[styles.hint, { color: theme.colors.onSurfaceVariant }]}>
            {emptyHint}
          </Text>
        ) : null}
        <View style={styles.actions}>{children}</View>
      </View>
    );
  }

  if (status === 'error') {
    return (
      <View style={styles.center}>
        <View style={[styles.iconBadge, { backgroundColor: `${theme.colors.error}1A` }]}>
          <Icon source="alert-circle-outline" size={40} color={theme.colors.error} />
        </View>
        <Text variant="bodyMedium" style={styles.title}>
          {errorText ?? STR.unknownError}
        </Text>
        {onRetry ? (
          <Button mode="contained" icon="refresh" onPress={onRetry} style={styles.retry}>
            {STR.retry}
          </Button>
        ) : null}
      </View>
    );
  }

  return <>{children}</>;
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: spacing.xl },
  iconBadge: { width: 80, height: 80, borderRadius: radius.xl, alignItems: 'center', justifyContent: 'center' },
  title: { marginTop: spacing.lg, textAlign: 'center', fontWeight: '600' },
  hint: { marginTop: spacing.xs, textAlign: 'center', maxWidth: 280 },
  actions: { marginTop: spacing.lg, width: '100%', maxWidth: 280, gap: spacing.sm },
  retry: { marginTop: spacing.lg, borderRadius: radius.lg },
});
