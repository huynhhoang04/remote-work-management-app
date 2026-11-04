import { router, Stack, useLocalSearchParams } from 'expo-router';
import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Alert, ScrollView, StyleSheet, View } from 'react-native';
import { Card, Text, useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { AppButton } from '@/components/AppButton';
import { ControlledField } from '@/components/ControlledField';
import { useSnackbar } from '@/components/SnackbarProvider';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { useDeleteProject, useProject, useUpdateProject } from '@/features/projects/hooks';
import { projectSchema, type ProjectForm } from '@/schemas/project';
import { radius, spacing } from '@/theme/tokens';
import { STR } from '@/utils/constants';
import { zodResolver } from '@/utils/zodResolver';

/**
 * Cài đặt dự án: sửa thông tin, xoá (Owner).
 * @reference docs/plan-app/02-phase-2.md §5.3
 */
export default function ProjectSettingsScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const { show } = useSnackbar();
  const project = useProject(id);
  const updateProject = useUpdateProject(id);
  const deleteProject = useDeleteProject();

  const form = useForm<ProjectForm>({
    resolver: zodResolver(projectSchema),
    defaultValues: { name: '', description: '' },
  });

  useEffect(() => {
    if (project.data) {
      form.reset({ name: project.data.name, description: project.data.description ?? '' });
    }
  }, [project.data, form]);

  const onSave = form.handleSubmit(async (values) => {
    try {
      await updateProject.mutateAsync({
        name: values.name.trim(),
        description: values.description?.trim() || undefined,
      });
      show(STR.common.saved);
    } catch (error) {
      show(normalizeError(error).message);
    }
  });

  const onDelete = () => {
    Alert.alert(STR.projectsExtra.deleteProject, STR.projectsExtra.deleteConfirm, [
      { text: STR.cancel, style: 'cancel' },
      {
        text: STR.common.delete,
        style: 'destructive',
        onPress: async () => {
          try {
            await deleteProject.mutateAsync(id);
            show(STR.common.deleted);
            router.replace('/projects');
          } catch (error) {
            show(normalizeError(error).message);
          }
        },
      },
    ]);
  };

  const canManage = project.data?.role === 'OWNER' || project.data?.role === 'ADMIN';

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen options={{ title: STR.projectsExtra.settings }} />
      {project.isPending ? (
        <SkeletonList count={2} />
      ) : !project.data ? (
        <StateView status="error" errorText={normalizeError(project.error).message} onRetry={() => project.refetch()} />
      ) : !canManage ? (
        <StateView status="empty" icon="lock-outline" emptyText={STR.common.permissionDenied} />
      ) : (
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
          <Card mode="elevated" style={styles.card}>
            <Card.Content>
              <Text variant="titleSmall" style={styles.cardTitle}>
                {STR.projectsExtra.editTitle}
              </Text>
              <ControlledField
                control={form.control}
                name="name"
                label={STR.projects.name}
                error={form.formState.errors.name?.message}
                autoCapitalize="sentences"
                maxLength={100}
                leftIcon="folder-outline"
              />
              <ControlledField
                control={form.control}
                name="description"
                label={STR.projects.description}
                error={form.formState.errors.description?.message}
                autoCapitalize="sentences"
                maxLength={1000}
                multiline
              />
              <AppButton
                label={STR.save}
                icon="content-save-outline"
                onPress={onSave}
                loading={updateProject.isPending}
                style={styles.button}
              />
            </Card.Content>
          </Card>

          {project.data.role === 'OWNER' ? (
            <Card mode="elevated" style={[styles.card, { borderColor: theme.colors.error, borderWidth: StyleSheet.hairlineWidth }]}>
              <Card.Content>
                <Text variant="titleSmall" style={[styles.cardTitle, { color: theme.colors.error }]}>
                  {STR.projectsExtra.deleteProject}
                </Text>
                <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                  {STR.projectsExtra.deleteConfirm}
                </Text>
                <AppButton
                  label={STR.projectsExtra.deleteProject}
                  variant="danger"
                  icon="trash-can-outline"
                  onPress={onDelete}
                  loading={deleteProject.isPending}
                  style={styles.button}
                />
              </Card.Content>
            </Card>
          ) : null}
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  container: { padding: spacing.lg, gap: spacing.md, paddingBottom: spacing.xxl },
  card: { borderRadius: radius.lg },
  cardTitle: { fontWeight: '700', marginBottom: spacing.sm },
  button: { marginTop: spacing.md },
});
