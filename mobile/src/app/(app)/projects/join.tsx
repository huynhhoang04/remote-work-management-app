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
import { useJoinProject } from '@/features/projects/hooks';
import { joinProjectSchema, type JoinProjectForm } from '@/schemas/project';
import { spacing } from '@/theme/tokens';
import { STR } from '@/utils/constants';
import { zodResolver } from '@/utils/zodResolver';

/**
 * Tham gia dự án bằng mã mời (FR-B03).
 * @reference docs/plan-app/01-phase-1.md §5.7
 */
export default function JoinProjectScreen() {
  const theme = useTheme();
  const { show } = useSnackbar();
  const joinProject = useJoinProject();

  const { control, handleSubmit, setError, formState } = useForm<JoinProjectForm>({
    resolver: zodResolver(joinProjectSchema),
    defaultValues: { code: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      const project = await joinProject.mutateAsync(values.code.trim().toUpperCase());
      show(STR.projects.joined);
      router.replace(`/projects/${project.id}`);
    } catch (error) {
      const apiError = normalizeError(error);
      if (apiError.code === 'INVITE_INVALID') {
        setError('code', { message: STR.projects.invalidInvite });
      } else {
        show(apiError.message);
      }
    }
  });

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['bottom']}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.flex}>
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
          <ControlledField
            control={control}
            name="code"
            label={STR.projects.inviteCode}
            error={formState.errors.code?.message}
            helperText={STR.projects.inviteCodeHint}
            autoCapitalize="characters"
            maxLength={12}
          />
          <AppButton label={STR.projects.joinCta} onPress={onSubmit} loading={joinProject.isPending} />
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
