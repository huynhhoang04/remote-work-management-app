import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Badge, Icon, Text, TouchableRipple, useTheme } from 'react-native-paper';
import { radius, spacing } from '../theme/tokens';
import type { NotificationDto } from '../types/api';
import { formatRelative } from '../utils/format';
import { STR } from '../utils/constants';

const ICONS: Record<string, string> = {
  TASK_ASSIGNED: 'account-check-outline',
  MENTION: 'at',
  REMIND_1D: 'calendar-clock-outline',
  REMIND_1H: 'clock-alert-outline',
  OVERDUE: 'alert-circle-outline',
  MESSAGE: 'message-text-outline',
};

interface Props {
  notification: NotificationDto;
  onPress: (notification: NotificationDto) => void;
}

/** Một dòng trong trung tâm thông báo. */
export function NotificationRow({ notification, onPress }: Props) {
  const theme = useTheme();
  const unread = !notification.readAt;

  return (
    <TouchableRipple
      onPress={() => onPress(notification)}
      style={{ backgroundColor: unread ? `${theme.colors.primary}0F` : theme.colors.surface }}
    >
      <View style={[styles.row, { borderBottomColor: theme.colors.outline }]}>
      <View style={[styles.iconBadge, { backgroundColor: unread ? `${theme.colors.primary}1F` : theme.colors.surfaceVariant }]}>
        <Icon
          source={ICONS[notification.type] ?? 'bell-outline'}
          size={20}
          color={unread ? theme.colors.primary : theme.colors.onSurfaceVariant}
        />
      </View>

      <View style={styles.body}>
        <View style={styles.titleRow}>
          <Text variant="titleSmall" numberOfLines={1} style={styles.title}>
            {notification.title}
          </Text>
          {unread ? <Badge size={8} style={{ backgroundColor: theme.colors.primary }} /> : null}
        </View>
        <Text variant="bodySmall" numberOfLines={2} style={{ color: theme.colors.onSurfaceVariant }}>
          {notification.body}
        </Text>
        <View style={styles.meta}>
          <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
            {(STR.notifications.types as Record<string, string>)[notification.type] ?? notification.type}
          </Text>
          <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
            {formatRelative(notification.createdAt)}
          </Text>
        </View>
      </View>
      </View>
    </TouchableRipple>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: spacing.md,
    padding: spacing.lg,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  iconBadge: { width: 40, height: 40, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center' },
  body: { flex: 1, gap: 3 },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  title: { flex: 1, fontWeight: '600' },
  meta: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 2, gap: spacing.sm },
});
