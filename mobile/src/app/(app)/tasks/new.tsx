import { router, Stack, useLocalSearchParams } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { useSnackbar } from '@/components/SnackbarProvider';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { TaskForm } from '@/features/tasks/TaskForm';
import { useCreateTask } from '@/features/tasks/hooks';
import { useProject } from '@/features/projects/hooks';
import { spacing } from '@/theme/tokens';
import type { CreateTaskPayload } from '@/types/api';
import { STR } from '@/utils/constants';

/**
 * Tạo task trong một dự án.
 * @reference docs/plan-app/02-phase-2.md §5.4
 */
export default function CreateTaskScreen() {
  const { projectId } = useLocalSearchParams<{ projectId?: string }>();
  const theme = useTheme();
  const { show } = useSnackbar();
  const project = useProject(projectId ?? '');
  const createTask = useCreateTask();

  const onSubmit = async (payload: CreateTaskPayload) => {
    try {
      const task = await createTask.mutateAsync(payload);
      show(STR.common.created);
      router.replace(`/tasks/${task.id}`);
    } catch (error) {
      show(normalizeError(error).message);
    }
  };

  if (!projectId) {
    return <StateView status="error" errorText="Thiếu dự án" />;
  }

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen options={{ title: STR.tasks.createTitle }} />
      {project.isPending ? (
        <SkeletonList count={3} />
      ) : project.isError || !project.data ? (
        <StateView status="error" errorText={normalizeError(project.error).message} onRetry={() => project.refetch()} />
      ) : project.data.role === 'VIEWER' ? (
        <StateView status="empty" icon="lock-outline" emptyText={STR.common.permissionDenied} />
      ) : (
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
          <TaskForm projectId={projectId} submitting={createTask.isPending} onSubmit={onSubmit} />
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  container: { padding: spacing.lg, paddingBottom: spacing.xxl },
});
