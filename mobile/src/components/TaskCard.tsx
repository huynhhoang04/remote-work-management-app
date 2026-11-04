import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Card, Icon, Text, useTheme } from 'react-native-paper';
import { radius, spacing } from '../theme/tokens';
import type { TaskDto } from '../types/api';
import { formatDate, isOverdue } from '../utils/format';
import { AssigneeAvatars } from './AssigneeAvatars';
import { LabelChip, TaskPriorityChip } from './TaskChips';

interface Props {
  task: TaskDto;
  onPress?: (task: TaskDto) => void;
  onLongPress?: (task: TaskDto) => void;
  compact?: boolean;
}

/** Card task trong Kanban/danh sách — deadline đỏ khi quá hạn. */
export function TaskCard({ task, onPress, onLongPress, compact = false }: Props) {
  const theme = useTheme();
  const overdue = task.status !== 'DONE' && isOverdue(task.deadline);
  const doneSubtasks = task.subtasks.filter((item) => item.done).length;

  return (
    <Card
      mode="elevated"
      style={[styles.card, compact ? styles.compact : null]}
      onPress={() => onPress?.(task)}
      onLongPress={() => onLongPress?.(task)}
      delayLongPress={220}
    >
      <Card.Content style={styles.content}>
        <Text variant="titleSmall" numberOfLines={2} style={styles.title}>
          {task.title}
        </Text>

        {task.labels.length > 0 ? (
          <View style={styles.labels}>
            {task.labels.slice(0, 3).map((label) => (
              <LabelChip key={label.id} label={label} />
            ))}
          </View>
        ) : null}

        <View style={styles.footer}>
          <View style={styles.meta}>
            {task.deadline ? (
              <View style={styles.metaItem}>
                <Icon
                  source={overdue ? 'alert-circle-outline' : 'calendar-clock-outline'}
                  size={13}
                  color={overdue ? theme.colors.error : theme.colors.onSurfaceVariant}
                />
                <Text
                  variant="labelSmall"
                  style={{ color: overdue ? theme.colors.error : theme.colors.onSurfaceVariant }}
                >
                  {formatDate(task.deadline)}
                </Text>
              </View>
            ) : null}
            {task.subtasks.length > 0 ? (
              <View style={styles.metaItem}>
                <Icon source="checkbox-marked-outline" size={13} color={theme.colors.onSurfaceVariant} />
                <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
                  {doneSubtasks}/{task.subtasks.length}
                </Text>
              </View>
            ) : null}
            {task.commentCount > 0 ? (
              <View style={styles.metaItem}>
                <Icon source="comment-outline" size={13} color={theme.colors.onSurfaceVariant} />
                <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
                  {task.commentCount}
                </Text>
              </View>
            ) : null}
          </View>
          <AssigneeAvatars assignees={task.assignees} />
        </View>

        <View style={styles.priority}>
          <TaskPriorityChip priority={task.priority} />
        </View>
      </Card.Content>
    </Card>
  );
}

const styles = StyleSheet.create({
  card: { borderRadius: radius.md, marginBottom: spacing.sm },
  compact: { marginBottom: spacing.xs },
  content: { padding: spacing.md },
  title: { fontWeight: '600' },
  labels: { flexDirection: 'row', flexWrap: 'wrap', gap: 4, marginTop: spacing.xs },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: spacing.sm,
    gap: spacing.sm,
  },
  meta: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, flexWrap: 'wrap', flex: 1 },
  metaItem: { flexDirection: 'row', alignItems: 'center', gap: 2 },
  priority: { position: 'absolute', top: spacing.sm, right: spacing.sm, opacity: 0.9 },
});
