import { router, Stack, useLocalSearchParams } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { useTheme } from 'react-native-paper';
import { isApiError, normalizeError } from '@/api/errors';
import { useSnackbar } from '@/components/SnackbarProvider';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { TaskForm } from '@/features/tasks/TaskForm';
import { useTask, useUpdateTask } from '@/features/tasks/hooks';
import { useProject } from '@/features/projects/hooks';
import { spacing } from '@/theme/tokens';
import type { CreateTaskPayload } from '@/types/api';
import { STR } from '@/utils/constants';

/**
 * Sửa task — gửi kèm version để backend chống ghi đè lạc hậu.
 * @reference docs/plan-app/02-phase-2.md §5.4
 */
export default function EditTaskScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const { show } = useSnackbar();
  const task = useTask(id);
  const project = useProject(task.data?.projectId ?? '');
  const updateTask = useUpdateTask();

  const onSubmit = async (payload: CreateTaskPayload) => {
    const { projectId: _projectId, ...rest } = payload;
    void _projectId;
    try {
      await updateTask.mutateAsync({ taskId: id, payload: rest });
      show(STR.common.saved);
      router.back();
    } catch (error) {
      const apiError = normalizeError(error);
      if (isApiError(apiError) && apiError.code === 'TASK_VERSION_CONFLICT') {
        show(STR.tasks.versionConflict);
        void task.refetch();
      } else {
        show(apiError.message);
      }
    }
  };

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen options={{ title: STR.tasks.editTitle }} />
      {task.isPending ? (
        <SkeletonList count={3} />
      ) : task.isError || !task.data ? (
        <StateView status="error" errorText={normalizeError(task.error).message} onRetry={() => task.refetch()} />
      ) : project.data?.role === 'VIEWER' ? (
        <StateView status="empty" icon="lock-outline" emptyText={STR.common.permissionDenied} />
      ) : (
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
          <TaskForm
            projectId={task.data.projectId}
            initial={task.data}
            submitting={updateTask.isPending}
            onSubmit={onSubmit}
          />
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  container: { padding: spacing.lg, paddingBottom: spacing.xxl },
});
