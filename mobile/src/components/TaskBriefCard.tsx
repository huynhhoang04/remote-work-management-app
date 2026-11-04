import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Card, Icon, Text, useTheme } from 'react-native-paper';
import { radius, spacing } from '../theme/tokens';
import type { TaskBriefDto } from '../types/api';
import { formatDate, isOverdue } from '../utils/format';
import { TaskPriorityChip, TaskStatusChip } from './TaskChips';

interface Props {
  task: TaskBriefDto;
  projectName?: string;
  onPress: (task: TaskBriefDto) => void;
}

/** Card rút gọn cho màn "Hôm nay". */
export function TaskBriefCard({ task, projectName, onPress }: Props) {
  const theme = useTheme();
  const overdue = task.status !== 'DONE' && isOverdue(task.deadline);

  return (
    <Card mode="elevated" style={styles.card} onPress={() => onPress(task)}>
      <Card.Content style={styles.content}>
        <View style={[styles.stripe, { backgroundColor: overdue ? theme.colors.error : theme.colors.primary }]} />
        <View style={styles.body}>
          <Text variant="titleSmall" numberOfLines={2}>
            {task.title}
          </Text>
          <View style={styles.meta}>
            {projectName ? (
              <Text variant="labelSmall" numberOfLines={1} style={{ color: theme.colors.onSurfaceVariant, flexShrink: 1 }}>
                {projectName}
              </Text>
            ) : null}
            {task.deadline ? (
              <View style={styles.deadline}>
                <Icon
                  source="clock-alert-outline"
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
          </View>
          <View style={styles.chips}>
            <TaskStatusChip status={task.status} />
            <TaskPriorityChip priority={task.priority} />
          </View>
        </View>
        <Icon source="chevron-right" size={20} color={theme.colors.onSurfaceVariant} />
      </Card.Content>
    </Card>
  );
}

const styles = StyleSheet.create({
  card: { borderRadius: radius.md, marginBottom: spacing.sm },
  content: { flexDirection: 'row', alignItems: 'center', padding: spacing.md, gap: spacing.md },
  stripe: { width: 4, alignSelf: 'stretch', borderRadius: 2 },
  body: { flex: 1, gap: spacing.xs },
  meta: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, flexWrap: 'wrap' },
  deadline: { flexDirection: 'row', alignItems: 'center', gap: 2 },
  chips: { flexDirection: 'row', gap: spacing.xs, flexWrap: 'wrap' },
});
