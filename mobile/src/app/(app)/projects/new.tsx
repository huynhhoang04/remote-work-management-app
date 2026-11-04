import { router } from 'expo-router';
import React from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet } from 'react-native';
import { useForm } from 'react-hook-form';
import { useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { normalizeError } from '@/api/errors';
import { AppButton } from '@/components/AppButton';
import { ControlledField } from '@/components/ControlledField';
import { useSnackbar } from '@/components/SnackbarProvider';
import { useCreateProject } from '@/features/projects/hooks';
import { projectSchema, type ProjectForm } from '@/schemas/project';
import { spacing } from '@/theme/tokens';
import { STR } from '@/utils/constants';
import { zodResolver } from '@/utils/zodResolver';

/**
 * Tạo dự án (FR-B02) — người tạo trở thành OWNER.
 * @reference docs/plan-app/01-phase-1.md §5.6
 */
export default function CreateProjectScreen() {
  const theme = useTheme();
  const { show } = useSnackbar();
  const createProject = useCreateProject();

  const { control, handleSubmit, formState } = useForm<ProjectForm>({
    resolver: zodResolver(projectSchema),
    defaultValues: { name: '', description: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      const project = await createProject.mutateAsync({
        name: values.name.trim(),
        description: values.description?.trim() || undefined,
      });
      show(STR.projects.created);
      router.replace(`/projects/${project.id}`);
    } catch (error) {
      show(normalizeError(error).message);
    }
  });

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['bottom']}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.flex}>
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
          <ControlledField
            control={control}
            name="name"
            label={STR.projects.name}
            error={formState.errors.name?.message}
            autoCapitalize="sentences"
            maxLength={100}
          />
          <ControlledField
            control={control}
            name="description"
            label={STR.projects.description}
            error={formState.errors.description?.message}
            multiline
            maxLength={1000}
            autoCapitalize="sentences"
          />
          <AppButton label={STR.projects.createCta} onPress={onSubmit} loading={createProject.isPending} />
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  flex: { flex: 1 },
  container: { padding: spacing.lg, gap: spacing.sm },
});
