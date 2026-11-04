import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Chip, Text } from 'react-native-paper';
import { priorityColors, spacing, statusColors } from '../theme/tokens';
import type { LabelDto, TaskPriority, TaskStatus } from '../types/api';
import { STR } from '../utils/constants';

export function TaskStatusChip({ status }: { status: TaskStatus | string }) {
  const color = statusColors[status] ?? '#64748B';
  return (
    <Chip compact textStyle={[styles.text, { color }]} style={[styles.chip, { backgroundColor: `${color}1A` }]}>
      {(STR.tasks.statuses as Record<string, string>)[status] ?? status}
    </Chip>
  );
}

export function TaskPriorityChip({ priority }: { priority: TaskPriority | string }) {
  const color = priorityColors[priority] ?? '#64748B';
  return (
    <Chip compact textStyle={[styles.text, { color }]} style={[styles.chip, { backgroundColor: `${color}1A` }]}>
      {(STR.tasks.priorities as Record<string, string>)[priority] ?? priority}
    </Chip>
  );
}

export function LabelChip({ label }: { label: LabelDto }) {
  return (
    <View style={[styles.label, { backgroundColor: `${label.color}22`, borderColor: `${label.color}55` }]}>
      <View style={[styles.dot, { backgroundColor: label.color }]} />
      <Text variant="labelSmall" style={{ color: label.color }} numberOfLines={1}>
        {label.name}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  chip: { alignSelf: 'flex-start' },
  text: { fontSize: 11, fontWeight: '600' },
  label: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    borderRadius: 999,
    borderWidth: StyleSheet.hairlineWidth,
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
    alignSelf: 'flex-start',
  },
  dot: { width: 6, height: 6, borderRadius: 3 },
});
